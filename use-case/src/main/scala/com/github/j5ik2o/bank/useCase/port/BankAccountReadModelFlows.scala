package com.github.j5ik2o.bank.useCase.port

import akka.NotUsed
import akka.stream.scaladsl.{ Flow, Source }
import com.github.j5ik2o.bank.domain.model.BankAccountId
import com.github.j5ik2o.bank.useCase.BankAccountAggregateUseCase.Protocol.{
  ResolveBankAccountEventsRequest,
  ResolveBankAccountEventsResponse
}
import org.sisioh.baseunits.scala.money.Money
import org.sisioh.baseunits.scala.time.TimePoint

import scala.concurrent.ExecutionContext

trait BankAccountReadModelFlows {

  def resolveLastSeqNrSource(implicit ec: ExecutionContext): Source[Long, NotUsed]

  def openBankAccountFlow: Flow[(BankAccountId, String, Long, TimePoint), Int, NotUsed]

  def updateAccountFlow: Flow[(BankAccountId, String, Long, TimePoint), Int, NotUsed]

  def depositBankAccountFlow(implicit ec: ExecutionContext): Flow[(BankAccountId, Money, Long, TimePoint), Int, NotUsed]

  def withdrawBankAccountFlow(
      implicit ec: ExecutionContext
  ): Flow[(BankAccountId, Money, Long, TimePoint), Int, NotUsed]

  def closeBankAccountFlow: Flow[(BankAccountId, Long, TimePoint), Int, NotUsed]

  def resolveBankAccountEventByIdFlow(
      implicit ec: ExecutionContext
  ): Flow[ResolveBankAccountEventsRequest, ResolveBankAccountEventsResponse, NotUsed]
}
