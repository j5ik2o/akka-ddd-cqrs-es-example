package com.github.j5ik2o.bank.adaptor.controller

import java.util.Currency

import com.github.j5ik2o.bank.adaptor.dao.EventType
import com.github.j5ik2o.bank.domain.model.{ BankAccountId, BankAccountName }
import com.github.j5ik2o.bank.useCase.BankAccountAggregateUseCase
import com.github.j5ik2o.bank.useCase.BankAccountAggregateUseCase.Protocol
import com.github.j5ik2o.bank.useCase.BankAccountAggregateUseCase.Protocol._
import org.hashids.Hashids
import org.sisioh.baseunits.scala.money.Money

case class InvalidEncodeIdException(id: BankAccountId, cause: Option[Throwable])
    extends Exception(s"Failed to encode BankAccountId: $id")

object BankAccountConverter {

  import Routes._

  val convertToCreateUseCaseModel: (BankAccountId, OpenBankAccountRequestJson) => OpenBankAccountRequest = {
    case (bankAccountId, json) =>
      BankAccountAggregateUseCase.Protocol.OpenBankAccountRequest(bankAccountId, BankAccountName(json.name))
  }

  def convertToCreateInterfaceModel(
      implicit hashIds: Hashids
  ): PartialFunction[BankAccountAggregateUseCase.Protocol.OpenBankAccountResponse, OpenBankAccountResponseJson] = {
    case response: BankAccountAggregateUseCase.Protocol.OpenBankAccountSucceeded =>
      response.bankAccountId.value.encodeToHashid.fold({ ex =>
        throw InvalidEncodeIdException(response.bankAccountId, Some(ex))
      }, { bankAccountId =>
        OpenBankAccountResponseJson(bankAccountId)
      })
    case response: BankAccountAggregateUseCase.Protocol.OpenBankAccountFailed =>
      response.bankAccountId.value.encodeToHashid.fold(
        { ex =>
          throw InvalidEncodeIdException(response.bankAccountId, Some(ex))
        }, { bankAccountId =>
          OpenBankAccountResponseJson(bankAccountId, Seq(response.error.message))
        }
      )
  }

  val convertToUpdateUseCaseModel: (BankAccountId, UpdateBankAccountRequestJson) => UpdateBankAccountRequest = {
    case (bankAccountId, json) =>
      BankAccountAggregateUseCase.Protocol.UpdateBankAccountRequest(bankAccountId, BankAccountName(json.name))
  }

  def convertToUpdateInterfaceModel(
      implicit hashIds: Hashids
  ): PartialFunction[BankAccountAggregateUseCase.Protocol.UpdateBankAccountResponse, UpdateBankAccountResponseJson] = {
    case response: BankAccountAggregateUseCase.Protocol.UpdateBankAccountSucceeded =>
      response.bankAccountId.value.encodeToHashid.fold({ ex =>
        throw InvalidEncodeIdException(response.bankAccountId, Some(ex))
      }, { bankAccountId =>
        UpdateBankAccountResponseJson(bankAccountId)
      })
    case response: BankAccountAggregateUseCase.Protocol.UpdateBankAccountFailed =>
      response.bankAccountId.value.encodeToHashid.fold(
        { ex =>
          throw InvalidEncodeIdException(response.bankAccountId, Some(ex))
        }, { bankAccountId =>
          UpdateBankAccountResponseJson(bankAccountId, Seq(response.ex.message))
        }
      )
  }

  val convertToDestroyUseCaseModel: (BankAccountId) => CloseBankAccountRequest = { bankAccountId =>
    BankAccountAggregateUseCase.Protocol.CloseBankAccountRequest(bankAccountId)
  }

  def convertToDestroyInterfaceModel(
      implicit hashIds: Hashids
  ): PartialFunction[BankAccountAggregateUseCase.Protocol.CloseBankAccountResponse, CloseBankAccountResponseJson] = {
    case response: BankAccountAggregateUseCase.Protocol.CloseBankAccountSucceeded =>
      response.bankAccountId.value.encodeToHashid.fold({ ex =>
        throw InvalidEncodeIdException(response.bankAccountId, Some(ex))
      }, { bankAccountId =>
        CloseBankAccountResponseJson(bankAccountId)
      })
    case response: BankAccountAggregateUseCase.Protocol.CloseBankAccountFailed =>
      response.bankAccountId.value.encodeToHashid.fold(
        { ex =>
          throw InvalidEncodeIdException(response.bankAccountId, Some(ex))
        }, { bankAccountId =>
          CloseBankAccountResponseJson(bankAccountId, Seq(response.error.message))
        }
      )
  }

  val convertToAddBankAccountEventUseCaseModel
    : (BankAccountId, AddBankAccountEventRequestJson) => Protocol.AddBankAccountEventRequest = {
    (id: BankAccountId, json: AddBankAccountEventRequestJson) =>
      EventType.withName(json.`type`) match {
        case EventType.Deposit =>
          BankAccountAggregateUseCase.Protocol
            .DepositRequest(id, Money(BigDecimal(json.amount), Currency.getInstance(json.currencyCode)))
        case EventType.Withdraw =>
          BankAccountAggregateUseCase.Protocol
            .WithdrawRequest(id, Money(BigDecimal(json.amount), Currency.getInstance(json.currencyCode)))
      }
  }

  def convertToAddBankAccountEventInterfaceModel(
      implicit hashIds: Hashids
  ): PartialFunction[BankAccountAggregateUseCase.Protocol.AddBankAccountEventResponse,
                     AddBankAccountEventResponseJson] = {
    case response: BankAccountAggregateUseCase.Protocol.DepositSucceeded =>
      response.bankAccountId.value.encodeToHashid.fold({ ex =>
        throw InvalidEncodeIdException(response.bankAccountId, Some(ex))
      }, { bankAccountId =>
        AddBankAccountEventResponseJson(bankAccountId)
      })
    case response: BankAccountAggregateUseCase.Protocol.DepositFailed =>
      response.bankAccountId.value.encodeToHashid.fold(
        { ex =>
          throw InvalidEncodeIdException(response.bankAccountId, Some(ex))
        }, { bankAccountId =>
          AddBankAccountEventResponseJson(bankAccountId, Seq(response.error.message))
        }
      )
    case response: BankAccountAggregateUseCase.Protocol.WithdrawSucceeded =>
      response.bankAccountId.value.encodeToHashid.fold({ ex =>
        throw InvalidEncodeIdException(response.bankAccountId, Some(ex))
      }, { bankAccountId =>
        AddBankAccountEventResponseJson(bankAccountId)
      })
    case response: BankAccountAggregateUseCase.Protocol.WithdrawFailed =>
      response.bankAccountId.value.encodeToHashid.fold(
        { ex =>
          throw InvalidEncodeIdException(response.bankAccountId, Some(ex))
        }, { bankAccountId =>
          AddBankAccountEventResponseJson(bankAccountId, Seq(response.error.message))
        }
      )
  }

  def convertToResolveInterfaceModel(
      implicit hashIds: Hashids
  ): PartialFunction[BankAccountAggregateUseCase.Protocol.ResolveBankAccountEventsResponse,
                     ResolveBankAccountEventsResponseJson] = {
    case response: ResolveBankAccountEventsSucceeded =>
      response.bankAccountId.value.encodeToHashid.fold(
        { ex =>
          throw InvalidEncodeIdException(response.bankAccountId, Some(ex))
        }, { bankAccountId =>
          ResolveBankAccountEventsResponseJson(
            bankAccountId,
            response.events.map(v => BankAccountEventJson(v.`type`, v.amount, v.currencyCode, v.createAt.toEpochSecond))
          )
        }
      )
    case response: ResolveBankAccountEventsFailed =>
      response.bankAccountId.value.encodeToHashid.fold(
        { ex =>
          throw InvalidEncodeIdException(response.bankAccountId, Some(ex))
        }, { bankAccountId =>
          ResolveBankAccountEventsResponseJson(
            bankAccountId,
            Seq.empty,
            Seq(response.error.message)
          )
        }
      )
  }

}
