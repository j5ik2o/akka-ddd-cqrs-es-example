package com.github.j5ik2o.bank.adaptor.dao

import akka.NotUsed
import akka.stream.scaladsl.{ Flow, Source }
import com.github.j5ik2o.bank.adaptor.generator.IdGenerator
import com.github.j5ik2o.bank.domain.model._
import com.github.j5ik2o.bank.useCase.BankAccountAggregateUseCase.Protocol.{
  BankAccountEventBody,
  ResolveBankAccountEventsRequest,
  ResolveBankAccountEventsResponse,
  ResolveBankAccountEventsSucceeded
}
import com.github.j5ik2o.bank.useCase.port.BankAccountReadModelFlows
import org.sisioh.baseunits.scala.money.Money
import org.sisioh.baseunits.scala.time.TimePoint
import slick.jdbc.JdbcProfile

import scala.concurrent.ExecutionContext

class BankAccountReadModelFlowsImpl(val profile: JdbcProfile, val db: JdbcProfile#Backend#Database)
    extends BankAccountComponent
    with BankAccountEventComponent
    with BankAccountReadModelFlows {
  import profile.api._

  private val bankAccountEventIdGenerator: IdGenerator[BankAccountEventId] =
    IdGenerator.ofBankAccountEventId(profile, db)

  def resolveLastSeqNrSource(implicit ec: ExecutionContext): Source[Long, NotUsed] =
    Source.single(1).mapAsync(1) { _ =>
      db.run(BankAccountDao.map(_.sequenceNr).max.result)
        .map(_.getOrElse(0L))
    }

  override def openBankAccountFlow: Flow[(BankAccountId, String, Long, TimePoint), Int, NotUsed] =
    Flow[(BankAccountId, String, Long, TimePoint)].mapAsync(1) {
      case (id, name, sequenceNr, occurredAt) =>
        db.run(
          BankAccountDao.forceInsert(
            BankAccountRecord(
              id.value,
              deleted = false,
              name,
              sequenceNr,
              occurredAt.asJavaZonedDateTime(),
              occurredAt.asJavaZonedDateTime()
            )
          )
        )
    }

  override def updateAccountFlow: Flow[(BankAccountId, String, Long, TimePoint), Int, NotUsed] =
    Flow[(BankAccountId, String, Long, TimePoint)].mapAsync(1) {
      case (id, name, sequenceNr, occurredAt) =>
        db.run(
          BankAccountDao
            .filter(_.id === id.value)
            .map(e => (e.name, e.sequenceNr, e.updatedAt))
            .update((name, sequenceNr, occurredAt.asJavaZonedDateTime()))
        )
    }

  override def depositBankAccountFlow(
      implicit ec: ExecutionContext
  ): Flow[(BankAccountId, Money, Long, TimePoint), Int, NotUsed] =
    Flow[(BankAccountId, Money, Long, TimePoint)].mapAsync(1) {
      case (id, deposit, sequenceNr, occurredAt) =>
        bankAccountEventIdGenerator.generateId().flatMap { bankAccountEventId =>
          val query = (for {
            bankAccountUpdateResult <- BankAccountDao
              .filter(_.id === id.value)
              .map(e => (e.sequenceNr, e.updatedAt))
              .update((sequenceNr, occurredAt.asJavaZonedDateTime()))
            bankAccountEventInsertResult <- BankAccountEventDao.forceInsert(
              BankAccountEventRecord(
                bankAccountEventId.value,
                id.value,
                EventType.Deposit.entryName,
                deposit.amount.toLong,
                deposit.currency.getCurrencyCode,
                occurredAt.asJavaZonedDateTime()
              )
            )
          } yield (bankAccountUpdateResult, bankAccountEventInsertResult)).transactionally
          db.run(query).map(_ => 1)
        }
    }

  override def withdrawBankAccountFlow(
      implicit ec: ExecutionContext
  ): Flow[(BankAccountId, Money, Long, TimePoint), Int, NotUsed] =
    Flow[(BankAccountId, Money, Long, TimePoint)].mapAsync(1) {
      case (id, withdraw, sequenceNr, occurredAt) =>
        bankAccountEventIdGenerator.generateId().flatMap { bankAccountEventId =>
          val query = for {
            bankAccountUpdateResult <- BankAccountDao
              .filter(_.id === id.value)
              .map(e => (e.sequenceNr, e.updatedAt))
              .update((sequenceNr, occurredAt.asJavaZonedDateTime()))
            bankAccountEventInsertResult <- BankAccountEventDao.forceInsert(
              BankAccountEventRecord(
                bankAccountEventId.value,
                id.value,
                EventType.Withdraw.entryName,
                withdraw.amount.toLong,
                withdraw.currency.getCurrencyCode,
                occurredAt.asJavaZonedDateTime()
              )
            )
          } yield (bankAccountUpdateResult, bankAccountEventInsertResult)
          db.run(query).map(_ => 1)
        }
    }

  override def closeBankAccountFlow: Flow[(BankAccountId, Long, TimePoint), Int, NotUsed] =
    Flow[(BankAccountId, Long, TimePoint)].mapAsync(1) {
      case (id, sequenceNr, occurredAt) =>
        db.run(
          BankAccountDao
            .filter(_.id === id.value)
            .map(e => (e.deleted, e.sequenceNr, e.updatedAt))
            .update((true, sequenceNr, occurredAt.asJavaZonedDateTime()))
        )
    }

  override def resolveBankAccountEventByIdFlow(
      implicit ec: ExecutionContext
  ): Flow[ResolveBankAccountEventsRequest, ResolveBankAccountEventsResponse, NotUsed] =
    Flow[ResolveBankAccountEventsRequest]
      .mapAsync(1) { _request =>
        db.run(BankAccountEventDao.filter(_.bankAccountId === _request.bankAccountId.value).result).map((_request, _))
      }
      .map {
        case (_request, result) =>
          val values = result.map { v =>
            BankAccountEventBody(v.`type`, v.amount, v.currencyCode, v.createdAt)
          }
          ResolveBankAccountEventsSucceeded(_request.bankAccountId, values)
      }
}
