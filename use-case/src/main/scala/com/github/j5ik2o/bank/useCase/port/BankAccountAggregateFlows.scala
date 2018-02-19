package com.github.j5ik2o.bank.useCase.port

import akka.NotUsed
import akka.stream.scaladsl.Flow
import com.github.j5ik2o.bank.useCase.BankAccountAggregateUseCase.Protocol._

trait BankAccountAggregateFlows {

  def openBankAccountFlow: Flow[OpenBankAccountRequest, OpenBankAccountResponse, NotUsed]

  def updateBankAccountFlow: Flow[UpdateBankAccountRequest, UpdateBankAccountResponse, NotUsed]

  def addBankAccountEventFlow: Flow[AddBankAccountEventRequest, AddBankAccountEventResponse, NotUsed]

  def closeBankAccountFlow: Flow[CloseBankAccountRequest, CloseBankAccountResponse, NotUsed]

}
