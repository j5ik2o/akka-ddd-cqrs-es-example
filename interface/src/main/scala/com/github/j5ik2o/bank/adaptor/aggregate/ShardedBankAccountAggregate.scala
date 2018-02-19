package com.github.j5ik2o.bank.adaptor.aggregate

import akka.actor._
import akka.cluster.sharding.ShardRegion
import akka.cluster.sharding.ShardRegion.Passivate
import com.github.j5ik2o.bank.adaptor.aggregate.BankAccountAggregate.Protocol.BankAccountCommandRequest
import com.github.j5ik2o.bank.domain.model.BankAccountId

object ShardedBankAccountAggregate {

  def props: Props = Props(new ShardedBankAccountAggregate)

  def name(id: BankAccountId): String = id.value.toString

  val shardName = "bank-accounts"

  case object StopBankAccount

  val extractEntityId: ShardRegion.ExtractEntityId = {
    case cmd: BankAccountCommandRequest => (cmd.bankAccountId.value.toString, cmd)
  }

  val extractShardId: ShardRegion.ExtractShardId = {
    case cmd: BankAccountCommandRequest => (cmd.bankAccountId.value % 12).toString
  }

}

class ShardedBankAccountAggregate extends BankAccountAggregate {
  import ShardedBankAccountAggregate._

  override def unhandled(message: Any): Unit = message match {
    case ReceiveTimeout =>
      context.parent ! Passivate(stopMessage = StopBankAccount)
    case StopBankAccount =>
      context.stop(self)
  }

}
