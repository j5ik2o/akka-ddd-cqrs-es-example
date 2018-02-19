package com.github.j5ik2o.bank.adaptor.aggregate

import akka.actor.{ Actor, ActorContext, ActorRef }
import com.github.j5ik2o.bank.adaptor.aggregate.BankAccountAggregate.Protocol.BankAccountCommandRequest
import com.github.j5ik2o.bank.domain.model.BankAccountId

trait BankAccountAggregatesLookup {
  implicit def context: ActorContext

  def forwardToBankAccountAggregate: Actor.Receive = {
    case cmd: BankAccountCommandRequest =>
      context
        .child(BankAccountAggregate.name(cmd.bankAccountId))
        .fold(createAndForward(cmd, cmd.bankAccountId))(forwardCommand(cmd))
  }

  def forwardCommand(cmd: BankAccountCommandRequest)(bankAccount: ActorRef): Unit =
    bankAccount forward cmd

  def createAndForward(cmd: BankAccountCommandRequest, bankAccountId: BankAccountId): Unit = {
    createBankAccountAggregate(bankAccountId) forward cmd
  }

  def createBankAccountAggregate(bankAccountId: BankAccountId): ActorRef =
    context.actorOf(BankAccountAggregate.props, BankAccountAggregate.name(bankAccountId))
}
