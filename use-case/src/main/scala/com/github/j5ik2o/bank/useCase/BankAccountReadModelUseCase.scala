package com.github.j5ik2o.bank.useCase

import akka.actor.ActorSystem
import akka.stream.scaladsl.{ Flow, Keep, Sink, Source, SourceQueueWithComplete }
import akka.stream.{ ActorMaterializer, OverflowStrategy }
import akka.{ Done, NotUsed }
import com.github.j5ik2o.bank.domain.model._
import com.github.j5ik2o.bank.useCase.BankAccountAggregateUseCase.Protocol.{
  ResolveBankAccountEventsRequest,
  ResolveBankAccountEventsResponse
}
import com.github.j5ik2o.bank.useCase.port.{ BankAccountReadModelFlows, JournalReader }
import pureconfig._

import scala.concurrent.{ ExecutionContext, ExecutionContextExecutor, Future, Promise }

class BankAccountReadModelUseCase(bankAccountReadModelFlows: BankAccountReadModelFlows, journalReader: JournalReader)(
    implicit val system: ActorSystem
) extends UseCaseSupport {

  import UseCaseSupport._

  private val config = loadConfigOrThrow[BankAccountAggregateUseCaseConfig]("bank.use-case.bank-account-use-case")

  private val bufferSize: Int = config.bufferSize

  private implicit val mat: ActorMaterializer       = ActorMaterializer()
  private implicit val ec: ExecutionContextExecutor = system.dispatcher

  def resolveBankAccountEventsById(
      request: ResolveBankAccountEventsRequest
  )(implicit ec: ExecutionContext): Future[ResolveBankAccountEventsResponse] =
    offerToQueue(resolveBankAccountEventQueue)(request, Promise())

  private lazy val resolveBankAccountEventQueue
    : SourceQueueWithComplete[(ResolveBankAccountEventsRequest, Promise[ResolveBankAccountEventsResponse])] =
    Source
      .queue[(ResolveBankAccountEventsRequest, Promise[ResolveBankAccountEventsResponse])](bufferSize,
                                                                                           OverflowStrategy.dropNew)
      .via(bankAccountReadModelFlows.resolveBankAccountEventByIdFlow.zipPromise)
      .toMat(completePromiseSink)(Keep.left)
      .run()

  private val projectionFlow: Flow[(BankAccountEvent, Long), Int, NotUsed] =
    Flow[(BankAccountEvent, Long)].flatMapConcat {
      case (event: BankAccountOpened, sequenceNr: Long) =>
        Source
          .single((event.bankAccountId, event.name.value, sequenceNr, event.occurredAt))
          .via(bankAccountReadModelFlows.openBankAccountFlow)
      case (event: BankAccountEventUpdated, sequenceNr: Long) =>
        Source
          .single((event.bankAccountId, event.name.value, sequenceNr, event.occurredAt))
          .via(bankAccountReadModelFlows.updateAccountFlow)
      case (event: BankAccountDeposited, sequenceNr: Long) =>
        Source
          .single((event.bankAccountId, event.deposit, sequenceNr, event.occurredAt))
          .via(bankAccountReadModelFlows.depositBankAccountFlow)
      case (event: BankAccountWithdrawn, sequenceNr: Long) =>
        Source
          .single((event.bankAccountId, event.withdraw, sequenceNr, event.occurredAt))
          .via(bankAccountReadModelFlows.withdrawBankAccountFlow)
      case (event: BankAccountClosed, sequenceNr: Long) =>
        Source
          .single((event.bankAccountId, sequenceNr, event.occurredAt))
          .via(bankAccountReadModelFlows.closeBankAccountFlow)
    }

  def execute(): Future[Done] = {
    bankAccountReadModelFlows.resolveLastSeqNrSource
      .flatMapConcat { lastSeqNr =>
        journalReader.eventsByTagSource(classOf[BankAccountEvent].getName, lastSeqNr + 1)
      }
      .map { eventBody =>
        (eventBody.event.asInstanceOf[BankAccountEvent], eventBody.sequenceNr)
      }
      .via(projectionFlow)
      .toMat(Sink.ignore)(Keep.right)
      .run()

  }
}
