package com.github.j5ik2o.bank.useCase

import java.time.ZonedDateTime

import akka.actor.ActorSystem
import akka.stream._
import akka.stream.scaladsl.{ Keep, Source, SourceQueueWithComplete }
import com.github.j5ik2o.bank.domain.model.BankAccount.BankAccountError
import com.github.j5ik2o.bank.domain.model.{ BankAccountId, BankAccountName }
import com.github.j5ik2o.bank.useCase.port.BankAccountAggregateFlows
import org.sisioh.baseunits.scala.money.Money
import pureconfig._

import scala.concurrent.{ ExecutionContext, Future, Promise }

object BankAccountAggregateUseCase {

  object Protocol {

    sealed trait BankAccountCommandRequest {
      val bankAccountId: BankAccountId
    }

    sealed trait BankAccountCommandResponse {
      val bankAccountId: BankAccountId
    }

    // ---

    case class OpenBankAccountRequest(bankAccountId: BankAccountId, name: BankAccountName)
        extends BankAccountCommandRequest

    sealed trait OpenBankAccountResponse {
      val bankAccountId: BankAccountId
    }

    case class OpenBankAccountSucceeded(bankAccountId: BankAccountId) extends OpenBankAccountResponse

    case class OpenBankAccountFailed(bankAccountId: BankAccountId, error: BankAccountError)
        extends OpenBankAccountResponse

    // ---

    case class UpdateBankAccountRequest(bankAccountId: BankAccountId, name: BankAccountName)
        extends BankAccountCommandRequest

    sealed trait UpdateBankAccountResponse {
      val bankAccountId: BankAccountId
    }

    case class UpdateBankAccountSucceeded(bankAccountId: BankAccountId) extends UpdateBankAccountResponse

    case class UpdateBankAccountFailed(bankAccountId: BankAccountId, ex: BankAccountError)
        extends UpdateBankAccountResponse

    // ---

    sealed trait AddBankAccountEventRequest  extends BankAccountCommandRequest
    sealed trait AddBankAccountEventResponse extends BankAccountCommandResponse

    case class DepositRequest(bankAccountId: BankAccountId, deposit: Money) extends AddBankAccountEventRequest

    sealed trait DepositResponse extends AddBankAccountEventResponse

    case class DepositSucceeded(bankAccountId: BankAccountId) extends DepositResponse

    case class DepositFailed(bankAccountId: BankAccountId, error: BankAccountError) extends DepositResponse

    // ---

    case class WithdrawRequest(bankAccountId: BankAccountId, withdraw: Money) extends AddBankAccountEventRequest

    sealed trait WithdrawResponse extends AddBankAccountEventResponse

    case class WithdrawSucceeded(bankAccountId: BankAccountId) extends WithdrawResponse

    case class WithdrawFailed(bankAccountId: BankAccountId, error: BankAccountError) extends WithdrawResponse

    // ---

    case class CloseBankAccountRequest(bankAccountId: BankAccountId) extends BankAccountCommandRequest

    sealed trait CloseBankAccountResponse {
      val bankAccountId: BankAccountId
    }

    case class CloseBankAccountSucceeded(bankAccountId: BankAccountId) extends CloseBankAccountResponse

    case class CloseBankAccountFailed(bankAccountId: BankAccountId, error: BankAccountError)
        extends CloseBankAccountResponse

    // ---

    case class GetBalanceRequest(bankAccountId: BankAccountId) extends BankAccountCommandRequest

    case class GetBalanceResponse(bankAccountId: BankAccountId, balance: Money) extends BankAccountCommandRequest

    // ---

    case class ResolveBankAccountEventsRequest(bankAccountId: BankAccountId) extends BankAccountCommandRequest

    sealed trait ResolveBankAccountEventsResponse extends BankAccountCommandResponse

    case class BankAccountEventBody(`type`: String, amount: Long, currencyCode: String, createAt: ZonedDateTime)

    case class ResolveBankAccountEventsSucceeded(bankAccountId: BankAccountId, events: Seq[BankAccountEventBody])
        extends ResolveBankAccountEventsResponse

    case class ResolveBankAccountEventsFailed(bankAccountId: BankAccountId, error: BankAccountError)
        extends ResolveBankAccountEventsResponse

  }

}

class BankAccountAggregateUseCase(bankAccountAggregateFlows: BankAccountAggregateFlows)(implicit system: ActorSystem)
    extends UseCaseSupport {
  import BankAccountAggregateUseCase.Protocol._
  import UseCaseSupport._

  implicit val mat: Materializer = ActorMaterializer()

  private val config = loadConfigOrThrow[BankAccountAggregateUseCaseConfig]("bank.use-case.bank-account-use-case")

  private val bufferSize: Int = config.bufferSize

  def openBankAccount(
      request: OpenBankAccountRequest
  )(implicit ec: ExecutionContext): Future[OpenBankAccountResponse] =
    offerToQueue(openBankAccountQueue)(request, Promise())

  def updateBankAccount(request: UpdateBankAccountRequest)(
      implicit ec: ExecutionContext
  ): Future[UpdateBankAccountResponse] = offerToQueue(updateBankAccountQueue)(request, Promise())

  def addBankAccountEvent(
      request: AddBankAccountEventRequest
  )(implicit ec: ExecutionContext): Future[AddBankAccountEventResponse] =
    offerToQueue(addBankAccountEventQueue)(request, Promise())

  def closeBankAccount(
      request: CloseBankAccountRequest
  )(implicit ec: ExecutionContext): Future[CloseBankAccountResponse] =
    offerToQueue(closeBankAccountQueue)(request, Promise())

  private val openBankAccountQueue
    : SourceQueueWithComplete[(OpenBankAccountRequest, Promise[OpenBankAccountResponse])] = Source
    .queue[(OpenBankAccountRequest, Promise[OpenBankAccountResponse])](bufferSize, OverflowStrategy.dropNew)
    .via(bankAccountAggregateFlows.openBankAccountFlow.zipPromise)
    .toMat(completePromiseSink)(Keep.left)
    .run()

  private val updateBankAccountQueue
    : SourceQueueWithComplete[(UpdateBankAccountRequest, Promise[UpdateBankAccountResponse])] = Source
    .queue[(UpdateBankAccountRequest, Promise[UpdateBankAccountResponse])](bufferSize, OverflowStrategy.dropNew)
    .via(bankAccountAggregateFlows.updateBankAccountFlow.zipPromise)
    .toMat(completePromiseSink)(Keep.left)
    .run()

  private val addBankAccountEventQueue
    : SourceQueueWithComplete[(AddBankAccountEventRequest, Promise[AddBankAccountEventResponse])] =
    Source
      .queue[(AddBankAccountEventRequest, Promise[AddBankAccountEventResponse])](bufferSize, OverflowStrategy.dropNew)
      .via(bankAccountAggregateFlows.addBankAccountEventFlow.zipPromise)
      .toMat(completePromiseSink)(Keep.left)
      .run()

  private val closeBankAccountQueue
    : SourceQueueWithComplete[(CloseBankAccountRequest, Promise[CloseBankAccountResponse])] = Source
    .queue[(CloseBankAccountRequest, Promise[CloseBankAccountResponse])](bufferSize, OverflowStrategy.dropNew)
    .via(bankAccountAggregateFlows.closeBankAccountFlow.zipPromise)
    .toMat(completePromiseSink)(Keep.left)
    .run()

}
