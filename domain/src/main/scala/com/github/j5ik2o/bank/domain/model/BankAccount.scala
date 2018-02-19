package com.github.j5ik2o.bank.domain.model

import java.util.{ Currency, Locale }

import com.github.j5ik2o.bank.domain.model.BankAccount.BankAccountError
import org.sisioh.baseunits.scala.money.Money
import org.sisioh.baseunits.scala.time.TimePoint
import org.sisioh.baseunits.scala.timeutil.Clock

trait BankAccount {

  def id: BankAccountId

  def name: BankAccountName

  def isClosed: Boolean

  def balance: Money

  def createdAt: TimePoint

  def updatedAt: TimePoint

  def close(occurredAt: TimePoint = Clock.now): Either[BankAccountError, BankAccount]

  def withName(name: BankAccountName, occurredAt: TimePoint = Clock.now): Either[BankAccountError, BankAccount]

  def deposit(money: Money, occurredAt: TimePoint = Clock.now): Either[BankAccountError, BankAccount]

  def withdraw(money: Money, occurredAt: TimePoint = Clock.now): Either[BankAccountError, BankAccount]

}

object BankAccount {
  final val DEFAULT_CURRENCY   = Currency.getInstance(Locale.getDefault())
  final val DEFAULT_MONEY_ZERO = Money.zero(BankAccount.DEFAULT_CURRENCY)

  sealed abstract class BankAccountError(val message: String)

  case class InvalidStateError(id: Option[BankAccountId] = None)
      extends BankAccountError(s"Invalid state${id.fold("")(id => s":id = ${id.value}")}")

  case class AlreadyClosedStateError(id: BankAccountId) extends BankAccountError(s"State is already closed: id = $id")

  case class DepositZeroError(id: BankAccountId, money: Money)
      extends BankAccountError(s"A deposited money amount 0 is illegal: id = $id, money = $money")

  case class NegativeBalanceError(id: BankAccountId, money: Money)
      extends BankAccountError(s"Forbidden that deposit amount to negative: id = $id, money = $money")

  def apply(id: BankAccountId,
            name: BankAccountName,
            isClosed: Boolean = false,
            balance: Money,
            createdAt: TimePoint,
            updatedAt: TimePoint): BankAccount = BankAccountImpl(id, name, isClosed, balance, createdAt, updatedAt)

  def unapply(self: BankAccount): Option[(BankAccountId, BankAccountName, Boolean, Money, TimePoint, TimePoint)] =
    Some((self.id, self.name, self.isClosed, self.balance, self.createdAt, self.updatedAt))

  private case class BankAccountImpl(id: BankAccountId,
                                     name: BankAccountName,
                                     isClosed: Boolean,
                                     balance: Money,
                                     createdAt: TimePoint,
                                     updatedAt: TimePoint)
      extends BankAccount {

    override def close(occurredAt: TimePoint): Either[BankAccountError, BankAccount] = {
      if (isClosed)
        Left(AlreadyClosedStateError(id))
      else
        Right(copy(isClosed = true, updatedAt = occurredAt))
    }

    override def withName(value: BankAccountName, occurredAt: TimePoint): Either[BankAccountError, BankAccount] = {
      if (isClosed)
        Left(AlreadyClosedStateError(id))
      else
        Right(copy(name = value, updatedAt = occurredAt))
    }

    override def deposit(money: Money, occurredAt: TimePoint): Either[BankAccountError, BankAccount] = {
      if (isClosed)
        Left(AlreadyClosedStateError(id))
      else
        money match {
          case d if d.isZero =>
            Left(DepositZeroError(id, money))
          case d if (balance + d).isNegative =>
            Left(NegativeBalanceError(id, money))
          case _ =>
            Right(copy(balance = balance + money, updatedAt = occurredAt))
        }
    }

    override def withdraw(money: Money, occurredAt: TimePoint): Either[BankAccountError, BankAccount] = {
      if (isClosed)
        Left(AlreadyClosedStateError(id))
      else
        money match {
          case d if d.isZero =>
            Left(DepositZeroError(id, money))
          case d if (balance - d).isNegative =>
            Left(NegativeBalanceError(id, money))
          case _ =>
            Right(copy(balance = balance - money, updatedAt = occurredAt))
        }
    }
  }

}
