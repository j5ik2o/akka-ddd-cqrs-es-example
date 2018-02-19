package com.github.j5ik2o.bank.adaptor.aggregate

import akka.actor.{ ActorLogging, Props }
import akka.persistence.{ PersistentActor, RecoveryCompleted, SaveSnapshotSuccess, SnapshotOffer }
import cats.implicits._
import com.github.j5ik2o.bank.domain.model.BankAccount.{ BankAccountError, InvalidStateError }
import com.github.j5ik2o.bank.domain.model._
import org.sisioh.baseunits.scala.money.Money
import pureconfig._

object BankAccountAggregate {

  def props: Props = Props(new BankAccountAggregate())

  def name(id: BankAccountId): String = id.value.toString

  final val AggregateName = "BankAccount"

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

    case class UpdateBankAccountSucceeded(bankAccountId: BankAccountId) extends OpenBankAccountResponse

    case class UpdateBankAccountFailed(bankAccountId: BankAccountId, error: BankAccountError)
        extends OpenBankAccountResponse

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

    case class DepositRequest(bankAccountId: BankAccountId, deposit: Money) extends BankAccountCommandRequest

    sealed trait DepositResponse extends BankAccountCommandResponse

    case class DepositSucceeded(bankAccountId: BankAccountId) extends DepositResponse

    case class DepositFailed(bankAccountId: BankAccountId, error: BankAccountError) extends DepositResponse

    // ---

    case class WithdrawRequest(bankAccountId: BankAccountId, withdraw: Money) extends BankAccountCommandRequest

    sealed trait WithdrawResponse extends BankAccountCommandResponse

    case class WithdrawSucceeded(bankAccountId: BankAccountId) extends WithdrawResponse

    case class WithdrawFailed(bankAccountId: BankAccountId, error: BankAccountError) extends WithdrawResponse

  }

  implicit class EitherOps(val self: Either[BankAccountError, BankAccount]) {
    def toSomeOrThrow: Option[BankAccount] = self.fold(error => throw new IllegalStateException(error.message), Some(_))
  }

}

class BankAccountAggregate extends PersistentActor with ActorLogging {

  import BankAccountAggregate.Protocol._
  import BankAccountAggregate._

  private val config = loadConfigOrThrow[BankAccountAggregateConfig](
    context.system.settings.config.getConfig("bank.interface.bank-account-aggregate")
  )

  context.setReceiveTimeout(config.receiveTimeout)

  override def persistenceId: String = s"$AggregateName-${self.path.name}"

  private var stateOpt: Option[BankAccount] = None

  private def tryToSaveSnapshot(id: BankAccountId): Unit =
    if (lastSequenceNr % config.numOfEventsToSnapshot == 0) {
      foreachState(saveSnapshot)
    }

  private def equalsId(requestId: BankAccountId): Boolean =
    stateOpt match {
      case None =>
        throw new IllegalStateException(s"Invalid state: requestId = $requestId")
      case Some(state) =>
        state.id == requestId
    }

  private def applyState(event: BankAccountOpened): Either[BankAccountError, BankAccount] =
    Either.right(
      BankAccount(event.bankAccountId,
                  event.name,
                  isClosed = false,
                  BankAccount.DEFAULT_MONEY_ZERO,
                  event.occurredAt,
                  event.occurredAt)
    )

  private def mapState(
      f: (BankAccount) => Either[BankAccountError, BankAccount]
  ): Either[BankAccountError, BankAccount] =
    for {
      state    <- Either.fromOption(stateOpt, InvalidStateError())
      newState <- f(state)
    } yield newState

  private def foreachState(f: (BankAccount) => Unit): Unit =
    Either.fromOption(stateOpt, InvalidStateError()).filterOrElse(!_.isClosed, InvalidStateError()).foreach(f)

  /**
    * Recovery handler that receives persisted events during recovery. If a state snapshot
    * has been captured and saved, this handler will receive a [[SnapshotOffer]] message
    * followed by events that are younger than the offered snapshot.
    */
  override def receiveRecover: Receive = {
    case event: BankAccountOpened =>
      stateOpt = applyState(event).toSomeOrThrow
    case event: BankAccountEventUpdated =>
      stateOpt = mapState(_.withName(event.name, event.occurredAt)).toSomeOrThrow
    case event: BankAccountDeposited =>
      stateOpt = mapState(_.deposit(event.deposit, event.occurredAt)).toSomeOrThrow
    case event: BankAccountWithdrawn =>
      stateOpt = mapState(_.withdraw(event.withdraw, event.occurredAt)).toSomeOrThrow
    case event: BankAccountClosed =>
      stateOpt = mapState(_.close(event.occurredAt)) toSomeOrThrow
    case SnapshotOffer(_, _state: BankAccount) =>
      stateOpt = Some(_state)
    case SaveSnapshotSuccess(metadata) =>
      log.debug(s"receiveRecover: SaveSnapshotSuccess succeeded: $metadata")
    case RecoveryCompleted =>
      log.debug(s"Recovery completed: $persistenceId")
  }

  /**
    * Command handler. Typically validates commands against current state (and/or by
    * communication with other actors). On successful validation, one or more events are
    * derived from a command and these events are then persisted by calling `persist`.
    */
  override def receiveCommand: Receive = {
    case GetBalanceRequest(bankAccountId) if equalsId(bankAccountId) =>
      foreachState { state =>
        sender() ! GetBalanceResponse(state.id, state.balance)
      }
    case OpenBankAccountRequest(bankAccountId, name) =>
      persist(BankAccountOpened(bankAccountId, name)) { event =>
        stateOpt = applyState(event).toSomeOrThrow
        sender() ! OpenBankAccountSucceeded(bankAccountId)
        tryToSaveSnapshot(bankAccountId)
      }
    case UpdateBankAccountRequest(bankAccountId, name) if equalsId(bankAccountId) =>
      mapState(_.withName(name)) match {
        case Left(error) =>
          sender() ! UpdateBankAccountFailed(bankAccountId, error)
        case Right(newState) =>
          persist(BankAccountEventUpdated(bankAccountId, name, newState.updatedAt)) { _ =>
            stateOpt = Some(newState)
            sender() ! UpdateBankAccountSucceeded(bankAccountId)
            tryToSaveSnapshot(bankAccountId)
          }
      }
    case DepositRequest(bankAccountId, deposit) if equalsId(bankAccountId) =>
      mapState(_.deposit(deposit)) match {
        case Left(error) =>
          sender() ! DepositFailed(bankAccountId, error)
        case Right(newState) =>
          persist(BankAccountDeposited(bankAccountId, deposit, newState.updatedAt)) { _ =>
            stateOpt = Some(newState)
            sender() ! DepositSucceeded(bankAccountId)
            tryToSaveSnapshot(bankAccountId)
          }
      }
    case WithdrawRequest(bankAccountId, withdraw) if equalsId(bankAccountId) =>
      mapState(_.withdraw(withdraw)) match {
        case Left(error) =>
          sender() ! WithdrawFailed(bankAccountId, error)
        case Right(newState) =>
          persist(BankAccountWithdrawn(bankAccountId, withdraw, newState.updatedAt)) { _ =>
            stateOpt = Some(newState)
            sender() ! WithdrawSucceeded(bankAccountId)
            tryToSaveSnapshot(bankAccountId)
          }
      }
    case CloseBankAccountRequest(bankAccountId) if equalsId(bankAccountId) =>
      mapState(_.close()) match {
        case Left(error) =>
          sender() ! CloseBankAccountFailed(bankAccountId, error)
        case Right(newState) =>
          persist(BankAccountClosed(bankAccountId, newState.updatedAt)) { _ =>
            stateOpt = Some(newState)
            sender() ! CloseBankAccountSucceeded(bankAccountId)
            tryToSaveSnapshot(bankAccountId)
          }
      }
    case SaveSnapshotSuccess(metadata) =>
      log.debug(s"receiveCommand: SaveSnapshotSuccess succeeded: $metadata")
  }
}
