package com.github.j5ik2o.bank.adaptor.cluster

import akka.actor.{ ActorIdentity, ActorRef, Identify, Props }
import akka.cluster.Cluster
import akka.persistence.Persistence
import akka.persistence.journal.leveldb.{ SharedLeveldbJournal, SharedLeveldbStore }
import akka.remote.testconductor.RoleName
import akka.remote.testkit.{ MultiNodeSpec, MultiNodeSpecCallbacks, MultiNodeConfig => AkkaMultiNodeConfig }
import akka.testkit.{ ImplicitSender, TestKit }
import com.github.j5ik2o.bank.adaptor.aggregate.BankAccountAggregate.Protocol.{
  OpenBankAccountRequest,
  OpenBankAccountSucceeded
}
import com.github.j5ik2o.bank.adaptor.aggregate.{ PersistenceCleanup, ShardedBankAccountAggregates }
import com.github.j5ik2o.bank.domain.model.{ BankAccountId, BankAccountName }
import com.typesafe.config.ConfigFactory
import org.scalatest.{ BeforeAndAfterAll, FreeSpecLike, Matchers }

import scala.concurrent.duration._

class AggregateActorMultiJvmNode1 extends ShardedBankAccountAggregatesSpec

class AggregateActorMultiJvmNode2 extends ShardedBankAccountAggregatesSpec

class AggregateActorMultiJvmNode3 extends ShardedBankAccountAggregatesSpec

trait STMultiNodeSpecSupport extends MultiNodeSpecCallbacks with FreeSpecLike with Matchers with BeforeAndAfterAll {

  override def beforeAll() = multiNodeSpecBeforeAll()

  override def afterAll() = multiNodeSpecAfterAll()

}

object MultiNodeSampleConfig extends AkkaMultiNodeConfig {
  val controller = role("controller")
  val node1      = role("node1")
  val node2      = role("node2")

  commonConfig(
    ConfigFactory.parseString(
      """
        |bank {
        |
        |  interface {
        |    bank-account-aggregate {
        |      receive-timeout = 15 s
        |      num-of-events-to-snapshot = 5
        |    }
        |    bank-account-event-json-serializer.is-debuged = true
        |    sharded-bank-account-aggregate {
        |      receive-timeout = 15 s
        |    }
        |  }
        |
        |  use-case {
        |    bank-account-use-case {
        |      buffer-size = 10
        |    }
        |  }
        |
        |}
        |
        |akka.cluster.metrics.enabled=off
        |akka.actor.provider = "akka.cluster.ClusterActorRefProvider"
        |akka.persistence.journal.plugin = "akka.persistence.journal.leveldb-shared"
        |akka.persistence.journal.leveldb-shared.store {
        |  native = off
        |  dir = "target/test-shared-journal"
        |}
        |akka.persistence.snapshot-store.plugin = "akka.persistence.snapshot-store.local"
        |akka.persistence.snapshot-store.local.dir = "target/test-snapshots"
      """.stripMargin
    )
  )

}

class ShardedBankAccountAggregatesSpec
    extends MultiNodeSpec(MultiNodeSampleConfig)
    with STMultiNodeSpecSupport
    with ImplicitSender
    with PersistenceCleanup {
  override def initialParticipants: Int = roles.size

  import MultiNodeSampleConfig._

  var sharedBankAccountAggregates: ActorRef = _

  override def beforeAll(): Unit = {
    deleteStorageLocations()
  }

  override def afterAll(): Unit = {
    deleteStorageLocations()
    TestKit.shutdownActorSystem(system)
  }

  def startSharding(): Unit = {
    ShardedBankAccountAggregates.start(system)
    sharedBankAccountAggregates = system.actorOf(ShardedBankAccountAggregates.props, ShardedBankAccountAggregates.name)
  }

  def join(from: RoleName, to: RoleName): Unit = {
    runOn(from) {
      Cluster(system) join node(to).address
      startSharding()
    }
    enterBarrier(from.name + "-joined")
  }

  "SharedBankAccountAggregates" - {
    "setup shared journal" in {
      // start the Persistence extension
      Persistence(system)
      runOn(controller) {
        system.actorOf(Props[SharedLeveldbStore], "store")
      }
      enterBarrier("controller started")
      runOn(node1, node2) {
        system.actorSelection(node(controller) / "user" / "store") ! Identify(None)
        val sharedStore = expectMsgType[ActorIdentity].ref.get
        SharedLeveldbJournal.setStore(sharedStore, system)
      }
      enterBarrier("shardJournal started")
    }
    "join cluster" in {
      within(15 seconds) {
        join(node1, node1)
        join(node2, node1)
        enterBarrier("cluster joined")
      }
    }
    "create bank account" in {
      within(15 seconds) {
        runOn(node1) {
          val bankAccountId = BankAccountId(1L)
          sharedBankAccountAggregates ! OpenBankAccountRequest(bankAccountId, BankAccountName("test-user-1"))
          expectMsgClass(classOf[OpenBankAccountSucceeded])
        }
        enterBarrier("create bank account 1")
        runOn(node2) {
          val bankAccountId = BankAccountId(2L)
          sharedBankAccountAggregates ! OpenBankAccountRequest(bankAccountId, BankAccountName("test-user-2"))
          expectMsgClass(classOf[OpenBankAccountSucceeded])
        }
        enterBarrier("create bank account 2")
      }
    }
  }

}
