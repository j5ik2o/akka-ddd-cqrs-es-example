package com.github.j5ik2o.bank.domain.model

import org.sisioh.baseunits.scala.money.Money
import org.sisioh.baseunits.scala.time.TimePoint
import org.sisioh.baseunits.scala.timeutil.Clock

sealed trait BankAccountEvent {
  val bankAccountId: BankAccountId
  val occurredAt: TimePoint
}

case class BankAccountOpened(bankAccountId: BankAccountId, name: BankAccountName, occurredAt: TimePoint = Clock.now)
    extends BankAccountEvent

case class BankAccountEventUpdated(bankAccountId: BankAccountId,
                                   name: BankAccountName,
                                   occurredAt: TimePoint = Clock.now)
    extends BankAccountEvent

case class BankAccountDeposited(bankAccountId: BankAccountId, deposit: Money, occurredAt: TimePoint = Clock.now)
    extends BankAccountEvent

case class BankAccountWithdrawn(bankAccountId: BankAccountId, withdraw: Money, occurredAt: TimePoint = Clock.now)
    extends BankAccountEvent

case class BankAccountClosed(bankAccountId: BankAccountId, occurredAt: TimePoint = Clock.now) extends BankAccountEvent
