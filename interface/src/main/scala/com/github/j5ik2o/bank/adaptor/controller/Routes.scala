package com.github.j5ik2o.bank.adaptor.controller

import akka.actor.ActorSystem
import akka.http.scaladsl.model.{ ContentTypes, HttpEntity, HttpResponse }
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.stream.Materializer
import cats.syntax.either._
import com.github.j5ik2o.bank.adaptor.generator.IdGenerator
import com.github.j5ik2o.bank.domain.model.BankAccountId
import com.github.j5ik2o.bank.useCase.{ BankAccountAggregateUseCase, BankAccountReadModelUseCase }
import org.hashids.Hashids

object Routes {

  trait ResponseJson {
    val isSuccessful: Boolean
    val errorMessages: Seq[String]
  }

  case class OpenBankAccountRequestJson(name: String)

  case class OpenBankAccountResponseJson(id: String, errorMessages: Seq[String] = Seq.empty) extends ResponseJson {
    override val isSuccessful: Boolean = errorMessages.isEmpty
  }

  case class UpdateBankAccountRequestJson(name: String)

  case class UpdateBankAccountResponseJson(id: String, errorMessages: Seq[String] = Seq.empty) extends ResponseJson {
    override val isSuccessful: Boolean = errorMessages.isEmpty
  }

  case class AddBankAccountEventRequestJson(`type`: String, amount: Long, currencyCode: String)

  case class AddBankAccountEventResponseJson(id: String, errorMessages: Seq[String] = Seq.empty) extends ResponseJson {
    override val isSuccessful: Boolean = errorMessages.isEmpty
  }

  case class CloseBankAccountRequestJson(id: String)

  case class CloseBankAccountResponseJson(id: String, errorMessages: Seq[String] = Seq.empty) extends ResponseJson {
    override val isSuccessful: Boolean = errorMessages.isEmpty
  }

  case class BankAccountEventJson(`type`: String, amount: Long, currencyCode: String, createAt: Long)

  case class ResolveBankAccountEventsResponseJson(id: String,
                                                  values: Seq[BankAccountEventJson],
                                                  errorMessages: Seq[String] = Seq.empty)
      extends ResponseJson {
    override val isSuccessful: Boolean = errorMessages.isEmpty
  }

  case class ValidationErrorsResponseJson(errorMessages: Seq[String]) extends ResponseJson {
    override val isSuccessful: Boolean = false
  }

  implicit class HashidsStringOps(val self: String) extends AnyVal {
    def decodeFromHashid(implicit hashIds: Hashids): Either[Throwable, Long] = {
      Either.catchNonFatal(hashIds.decode(self)(0))
    }
  }

  implicit class HashidsLongOps(val self: Long) extends AnyVal {
    def encodeToHashid(implicit hashIds: Hashids): Either[Throwable, String] =
      Either.catchNonFatal(hashIds.encode(self))
  }

}

case class Routes(bankAccountIdGenerator: IdGenerator[BankAccountId],
                  bankAccountAggregateUseCase: BankAccountAggregateUseCase,
                  bankAccountReadModelUseCase: BankAccountReadModelUseCase)(
    implicit system: ActorSystem,
    mat: Materializer
) extends BankAccountController {

  implicit val ec = system.dispatcher

  def root: Route = RouteLogging.default.httpLogRequestResult {
    pathEndOrSingleSlash {
      get {
        index()
      }
    } ~ toBankAccountRoutes
  }

  private def index() = complete(
    HttpResponse(
      entity = HttpEntity(
        ContentTypes.`text/plain(UTF-8)`,
        "Wellcome to Bank API"
      )
    )
  )

}
