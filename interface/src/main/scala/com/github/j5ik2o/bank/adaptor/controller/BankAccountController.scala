package com.github.j5ik2o.bank.adaptor.controller

import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server._
import com.github.j5ik2o.bank.adaptor.generator.IdGenerator
import com.github.j5ik2o.bank.domain.model.BankAccountId
import com.github.j5ik2o.bank.useCase.BankAccountAggregateUseCase.Protocol._
import com.github.j5ik2o.bank.useCase.{ BankAccountAggregateUseCase, BankAccountReadModelUseCase }
import de.heikoseeberger.akkahttpcirce.FailFastCirceSupport._
import io.circe.generic.auto._

import scala.concurrent.ExecutionContext

trait BankAccountController extends BankAccountValidateDirectives {
  import BankAccountConverter._
  import ControllerBase._
  import Routes._

  private val bankAccountsRouteName = "bank-accounts"

  protected val bankAccountAggregateUseCase: BankAccountAggregateUseCase

  protected val bankAccountReadModelUseCase: BankAccountReadModelUseCase

  protected val bankAccountIdGenerator: IdGenerator[BankAccountId]

  def toBankAccountRoutes(implicit ec: ExecutionContext): Route = handleRejections(RejectionHandlers.default) {
    openBankAccount ~ updateBankAccount ~ addBankAccountEvent ~ closeBankAccount ~ resolveBankAccountEventsById
  }

  def openBankAccount(implicit ec: ExecutionContext): Route = pathPrefix(bankAccountsRouteName) {
    pathEndOrSingleSlash {
      post {
        entity(as[OpenBankAccountRequestJson]) { json =>
          validateBankAccountRequestJson(json).apply { validatedJson =>
            val future = bankAccountIdGenerator
              .generateId()
              .map(id => (id, validatedJson))
              .map(convertToCreateUseCaseModel.tupled)
              .flatMap(bankAccountAggregateUseCase.openBankAccount)
              .map(convertToCreateInterfaceModel)
            onSuccess(future) { response =>
              complete(response)
            }
          }
        }
      }
    }
  }

  def updateBankAccount(implicit ec: ExecutionContext): Route = pathPrefix(bankAccountsRouteName / Segment) {
    idString =>
      pathEndOrSingleSlash {
        put {
          validateBankAccountId(idString) { id =>
            entity(as[UpdateBankAccountRequestJson]) { json =>
              validateBankAccountRequestJson(json).apply { validatedJson =>
                val future = bankAccountAggregateUseCase
                  .updateBankAccount(convertToUpdateUseCaseModel(id, validatedJson))
                  .map(convertToUpdateInterfaceModel)
                onSuccess(future) { response =>
                  complete(response)
                }
              }
            }
          }
        }
      }
  }

  def addBankAccountEvent(implicit ec: ExecutionContext): Route =
    pathPrefix(bankAccountsRouteName / Segment / "events") { idString =>
      pathEndOrSingleSlash {
        put {
          validateBankAccountId(idString) { id =>
            entity(as[AddBankAccountEventRequestJson]) { json =>
              validateBankAccountRequestJson(json).apply { validatedJson =>
                val future = bankAccountAggregateUseCase
                  .addBankAccountEvent(convertToAddBankAccountEventUseCaseModel(id, validatedJson))
                  .map(convertToAddBankAccountEventInterfaceModel)
                onSuccess(future) { response =>
                  complete(response)
                }
              }
            }
          }
        }
      }
    }

  def closeBankAccount(implicit ec: ExecutionContext): Route = pathPrefix(bankAccountsRouteName / Segment) { idString =>
    pathEndOrSingleSlash {
      delete {
        validateBankAccountId(idString) { id =>
          val future = bankAccountAggregateUseCase
            .closeBankAccount(CloseBankAccountRequest(id))
            .map(convertToDestroyInterfaceModel)
          onSuccess(future) { response =>
            complete(response)
          }
        }
      }
    }
  }

  def resolveBankAccountEventsById(implicit ec: ExecutionContext): Route =
    pathPrefix(bankAccountsRouteName / Segment) { idString =>
      pathEndOrSingleSlash {
        get {
          validateBankAccountId(idString) { id =>
            val future = bankAccountReadModelUseCase
              .resolveBankAccountEventsById(ResolveBankAccountEventsRequest(id))
              .map(convertToResolveInterfaceModel)
            onSuccess(future) { response =>
              complete(response)
            }
          }
        }
      }
    }

}
