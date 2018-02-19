package com.github.j5ik2o.bank.adaptor.serialization

import java.util.Currency

import com.github.j5ik2o.bank.domain.model._
import org.sisioh.baseunits.scala.money.Money
import org.sisioh.baseunits.scala.time.TimePoint

case class BankAccountCreatedJson(id: Long, name: String, occurredAt: Long)
case class BankAccountUpdatedJson(id: Long, name: String, occurredAt: Long)
case class BankAccountDepositedJson(id: Long, amount: Long, currencyCode: String, occurredAt: Long)
case class BankAccountWithdrawedJson(id: Long, amount: Long, currencyCode: String, occurredAt: Long)
case class BankAccountDestroyedJson(id: Long, occurredAt: Long)

object BankAccountCreatedJson {

  implicit object BankAccountCreatedIso extends EventToJsonReprIso[BankAccountOpened, BankAccountCreatedJson] {
    override def convertTo(event: BankAccountOpened): BankAccountCreatedJson = {
      BankAccountCreatedJson(
        id = event.bankAccountId.value,
        name = event.name.value,
        occurredAt = event.occurredAt.millisecondsFromEpoc
      )
    }

    override def convertFrom(json: BankAccountCreatedJson): BankAccountOpened = {
      BankAccountOpened(
        bankAccountId = BankAccountId(json.id),
        name = BankAccountName(json.name),
        occurredAt = TimePoint.from(json.occurredAt)
      )
    }
  }

  implicit object BankAccountUpdatedIso extends EventToJsonReprIso[BankAccountEventUpdated, BankAccountUpdatedJson] {
    override def convertTo(event: BankAccountEventUpdated): BankAccountUpdatedJson = {
      BankAccountUpdatedJson(
        id = event.bankAccountId.value,
        name = event.name.value,
        occurredAt = event.occurredAt.millisecondsFromEpoc
      )
    }

    override def convertFrom(json: BankAccountUpdatedJson): BankAccountEventUpdated = {
      BankAccountEventUpdated(
        bankAccountId = BankAccountId(json.id),
        name = BankAccountName(json.name),
        occurredAt = TimePoint.from(json.occurredAt)
      )
    }
  }

  implicit object BankAccountDepositedIso extends EventToJsonReprIso[BankAccountDeposited, BankAccountDepositedJson] {
    override def convertTo(event: BankAccountDeposited): BankAccountDepositedJson = {
      BankAccountDepositedJson(
        id = event.bankAccountId.value,
        amount = event.deposit.amount.toLong,
        currencyCode = event.deposit.currency.getCurrencyCode,
        occurredAt = event.occurredAt.millisecondsFromEpoc
      )
    }

    override def convertFrom(json: BankAccountDepositedJson): BankAccountDeposited = {
      BankAccountDeposited(
        bankAccountId = BankAccountId(json.id),
        deposit = Money.apply(BigDecimal(json.amount), Currency.getInstance(json.currencyCode)),
        occurredAt = TimePoint.from(json.occurredAt)
      )
    }
  }

  implicit object BankAccountWithdrawIso extends EventToJsonReprIso[BankAccountWithdrawn, BankAccountWithdrawedJson] {
    override def convertTo(event: BankAccountWithdrawn): BankAccountWithdrawedJson = {
      BankAccountWithdrawedJson(
        id = event.bankAccountId.value,
        amount = event.withdraw.amount.toLong,
        currencyCode = event.withdraw.currency.getCurrencyCode,
        occurredAt = event.occurredAt.millisecondsFromEpoc
      )
    }

    override def convertFrom(json: BankAccountWithdrawedJson): BankAccountWithdrawn = {
      BankAccountWithdrawn(
        bankAccountId = BankAccountId(json.id),
        withdraw = Money(BigDecimal(json.amount), Currency.getInstance(json.currencyCode)),
        occurredAt = TimePoint.from(json.occurredAt)
      )
    }
  }

  implicit object BankAccountDestroyedIso extends EventToJsonReprIso[BankAccountClosed, BankAccountDestroyedJson] {
    override def convertTo(event: BankAccountClosed): BankAccountDestroyedJson = {
      BankAccountDestroyedJson(
        id = event.bankAccountId.value,
        occurredAt = event.occurredAt.millisecondsFromEpoc
      )
    }

    override def convertFrom(json: BankAccountDestroyedJson): BankAccountClosed = {
      BankAccountClosed(
        bankAccountId = BankAccountId(json.id),
        occurredAt = TimePoint.from(json.occurredAt)
      )
    }
  }

}
