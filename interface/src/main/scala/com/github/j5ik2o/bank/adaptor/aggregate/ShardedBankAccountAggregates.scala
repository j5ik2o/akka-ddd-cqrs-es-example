package com.github.j5ik2o.bank.adaptor.aggregate

import akka.actor.{ Actor, ActorLogging, ActorRef, ActorSystem, Props }
import akka.cluster.sharding.{ ClusterSharding, ClusterShardingSettings }
import com.github.j5ik2o.bank.adaptor.aggregate.BankAccountAggregate.Protocol.BankAccountCommandRequest

object ShardedBankAccountAggregates {
  def props: Props = Props(new ShardedBankAccountAggregates())
  def name: String = "sharded-bank-accounts"

  def start(system: ActorSystem): ActorRef = {
    system.log.debug("ShardedBankAccounts#start: start")
    val actorRef = ClusterSharding(system).start(
      ShardedBankAccountAggregate.shardName,
      ShardedBankAccountAggregate.props,
      ClusterShardingSettings(system),
      ShardedBankAccountAggregate.extractEntityId,
      ShardedBankAccountAggregate.extractShardId
    )
    system.log.debug("ShardedBankAccounts#start: finish")
    actorRef
  }

  def shardRegion(system: ActorSystem): ActorRef =
    ClusterSharding(system).shardRegion(ShardedBankAccountAggregate.shardName)

}

class ShardedBankAccountAggregates extends Actor with ActorLogging {

  ShardedBankAccountAggregates.start(context.system)

  override def receive: Receive = {
    case cmd: BankAccountCommandRequest =>
      ShardedBankAccountAggregates.shardRegion(context.system) forward cmd
  }

}
