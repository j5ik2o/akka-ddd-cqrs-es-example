package com.github.j5ik2o.bank.adaptor.aggregate

import akka.actor.{ Actor, Props }

object LocalBankAccountAggregates {

  def props = Props(new LocalBankAccountAggregates)

  def name = "local-bank-accounts"

}

class LocalBankAccountAggregates extends Actor with BankAccountAggregatesLookup {
  override def receive: Receive = forwardToBankAccountAggregate
}
