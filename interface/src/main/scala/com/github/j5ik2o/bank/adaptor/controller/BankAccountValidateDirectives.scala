package com.github.j5ik2o.bank.adaptor.controller

import java.util.Currency

import akka.http.scaladsl.server.Directive1
import akka.http.scaladsl.server.Directives._
import cats.data.{ NonEmptyList, Validated, ValidatedNel }
import cats.implicits._
import com.github.j5ik2o.bank.adaptor.dao.EventType
import com.github.j5ik2o.bank.domain.model.{ BankAccountId, BankAccountName }
import org.sisioh.baseunits.scala.money.Money

sealed trait Error {
  val message: String
  val cause: Option[Throwable]
}

case class BankAccountIdFormatError(message: String, cause: Option[Throwable] = None)  extends Error
case class BankAccountNameError(message: String, cause: Option[Throwable] = None)      extends Error
case class BankAccountEventTypeError(message: String, cause: Option[Throwable] = None) extends Error
case class BankAccountMoneyError(message: String, cause: Option[Throwable] = None)     extends Error

object ValidateUtils {
  import ControllerBase._
  import Routes._

  type ValidationResult[A] = ValidatedNel[Error, A]

  def validateBankAccountId(value: String): Validated[Error, BankAccountId] = {
    value.decodeFromHashid.map(BankAccountId) match {
      case Left(error)   => BankAccountIdFormatError("The id format is invalid.", Some(error)).invalid
      case Right(result) => result.valid
    }
  }

  def validateBankAccountName(value: String): ValidationResult[BankAccountName] = {
    if (value.isEmpty || value.length > 255)
      BankAccountNameError("The name is empty or 255 length over.").invalidNel
    else BankAccountName(value).validNel
  }

  trait Validator[A] {
    def validate(value: A): ValidationResult[A]
  }

  implicit object OpenBankAccountRequestJsonValidator extends Validator[OpenBankAccountRequestJson] {
    override def validate(value: OpenBankAccountRequestJson): ValidationResult[OpenBankAccountRequestJson] =
      validateBankAccountName(value.name).map(_ => OpenBankAccountRequestJson(value.name))
  }

  implicit object UpdateBankAccountRequestJsonValidator extends Validator[UpdateBankAccountRequestJson] {
    override def validate(value: UpdateBankAccountRequestJson): ValidationResult[UpdateBankAccountRequestJson] = {
      validateBankAccountName(value.name).map(_ => UpdateBankAccountRequestJson(value.name))
    }
  }

  implicit object AddBankAccountEventRequestJsonValidator extends Validator[AddBankAccountEventRequestJson] {
    override def validate(value: AddBankAccountEventRequestJson): ValidationResult[AddBankAccountEventRequestJson] = {
      (validateEventType(value.`type`), validateMoney(value.amount, value.currencyCode)).mapN {
        case (_: EventType, _: Money) =>
          value
      }
    }
  }

  def validateEventType(value: String): ValidationResult[EventType] = {
    try {
      EventType.withName(value).validNel
    } catch {
      case ex: Throwable => BankAccountEventTypeError("", Some(ex)).invalidNel
    }
  }

  def validateMoney(amount: Long, currencyCode: String): ValidationResult[Money] = {
    try {
      Money(BigDecimal(amount), Currency.getInstance(currencyCode)).validNel
    } catch {
      case ex: Throwable => BankAccountMoneyError("", Some(ex)).invalidNel
    }
  }

}

trait BankAccountValidateDirectives {
  import ValidateUtils._

  protected def validateBankAccountId(value: String): Directive1[BankAccountId] = {
    ValidateUtils
      .validateBankAccountId(value)
      .fold({ error =>
        reject(ValidationsRejection(NonEmptyList.of(error)))
      }, provide)
  }

  protected def validateBankAccountRequestJson[A: Validator](value: A): Directive1[A] =
    implicitly[Validator[A]]
      .validate(value)
      .fold({ errors =>
        reject(ValidationsRejection(errors))
      }, provide)

}
