package com.github.j5ik2o.bank.adaptor.aggregate

import akka.NotUsed
import akka.actor.{ ActorRef, ActorSystem }
import akka.pattern.ask
import akka.stream.scaladsl.Flow
import akka.util.Timeout
import com.github.j5ik2o.bank.useCase.BankAccountAggregateUseCase.Protocol
import com.github.j5ik2o.bank.useCase.port.BankAccountAggregateFlows
import pureconfig._

class BankAccountAggregateFlowsImpl(aggregateRef: ActorRef)(
    implicit val system: ActorSystem
) extends BankAccountAggregateFlows {

  import Protocol._

  private val config = loadConfigOrThrow[BankAccountAggregateFlowsConfig](
    system.settings.config.getConfig("bank.interface.bank-account-aggregate-flows")
  )

  private implicit val to: Timeout = Timeout(config.callTimeout)

  override def openBankAccountFlow: Flow[OpenBankAccountRequest, OpenBankAccountResponse, NotUsed] =
    Flow[OpenBankAccountRequest]
      .map { request =>
        BankAccountAggregate.Protocol.OpenBankAccountRequest(request.bankAccountId, request.name)
      }
      .mapAsync(1)(aggregateRef ? _)
      .map {
        case response: BankAccountAggregate.Protocol.OpenBankAccountSucceeded =>
          OpenBankAccountSucceeded(response.bankAccountId)
        case response: BankAccountAggregate.Protocol.OpenBankAccountFailed =>
          OpenBankAccountFailed(response.bankAccountId, response.error)
      }

  override def updateBankAccountFlow: Flow[UpdateBankAccountRequest, UpdateBankAccountResponse, NotUsed] =
    Flow[UpdateBankAccountRequest]
      .map { request =>
        BankAccountAggregate.Protocol.UpdateBankAccountRequest(request.bankAccountId, request.name)
      }
      .mapAsync(1)(aggregateRef ? _)
      .map {
        case response: BankAccountAggregate.Protocol.UpdateBankAccountSucceeded =>
          UpdateBankAccountSucceeded(response.bankAccountId)
        case response: BankAccountAggregate.Protocol.UpdateBankAccountFailed =>
          UpdateBankAccountFailed(response.bankAccountId, response.error)
      }

  override def addBankAccountEventFlow: Flow[AddBankAccountEventRequest, AddBankAccountEventResponse, NotUsed] =
    Flow[AddBankAccountEventRequest]
      .map {
        case request: DepositRequest =>
          BankAccountAggregate.Protocol.DepositRequest(request.bankAccountId, request.deposit)
        case request: WithdrawRequest =>
          BankAccountAggregate.Protocol.WithdrawRequest(request.bankAccountId, request.withdraw)
      }
      .mapAsync(1)(aggregateRef ? _)
      .map {
        case response: BankAccountAggregate.Protocol.DepositSucceeded =>
          DepositSucceeded(response.bankAccountId)
        case response: BankAccountAggregate.Protocol.DepositFailed =>
          DepositFailed(response.bankAccountId, response.error)
        case response: BankAccountAggregate.Protocol.WithdrawSucceeded =>
          WithdrawSucceeded(response.bankAccountId)
        case response: BankAccountAggregate.Protocol.WithdrawFailed =>
          WithdrawFailed(response.bankAccountId, response.error)
      }

  override def closeBankAccountFlow: Flow[CloseBankAccountRequest, CloseBankAccountResponse, NotUsed] =
    Flow[CloseBankAccountRequest]
      .map { request =>
        BankAccountAggregate.Protocol.CloseBankAccountRequest(request.bankAccountId)
      }
      .mapAsync(1)(aggregateRef ? _)
      .map {
        case response: BankAccountAggregate.Protocol.CloseBankAccountSucceeded =>
          CloseBankAccountSucceeded(response.bankAccountId)
        case response: BankAccountAggregate.Protocol.CloseBankAccountFailed =>
          CloseBankAccountFailed(response.bankAccountId, response.error)
      }

}
