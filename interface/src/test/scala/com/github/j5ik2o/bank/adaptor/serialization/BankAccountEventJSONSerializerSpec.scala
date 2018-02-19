package com.github.j5ik2o.bank.adaptor.serialization

import akka.actor.ActorSystem
import akka.serialization.SerializationExtension
import com.github.j5ik2o.bank.adaptor.util.ActorSpec
import com.github.j5ik2o.bank.domain.model._
import com.typesafe.config.ConfigFactory
import org.sisioh.baseunits.scala.money.Money
import org.sisioh.baseunits.scala.timeutil.Clock

class BankAccountEventJSONSerializerSpec
    extends ActorSpec(
      ActorSystem("BankAccountEventJSONSerializerSpec", ConfigFactory.load("bank-account-aggregate-spec.conf"))
    ) {
  val extension = SerializationExtension(system)

  "BankAccountEventJSONSerializer" - {
    "should encode CreateEvent" in {
      val serializer    = extension.serializerFor(classOf[BankAccountOpened])
      val now           = Clock.now
      val expectedEvent = BankAccountOpened(BankAccountId(1L), BankAccountName("test-1"), now)
      val byteArray     = serializer.toBinary(expectedEvent)
      val event         = serializer.fromBinary(byteArray, Some(classOf[BankAccountOpened]))
      event shouldBe expectedEvent
    }
    "should encode UpdateEvent" in {
      val serializer    = extension.serializerFor(classOf[BankAccountEventUpdated])
      val now           = Clock.now
      val expectedEvent = BankAccountEventUpdated(BankAccountId(1L), BankAccountName("test-1"), now)
      val byteArray     = serializer.toBinary(expectedEvent)
      val event         = serializer.fromBinary(byteArray, Some(classOf[BankAccountEventUpdated]))
      event shouldBe expectedEvent
    }
    "should encode DepositEvent" in {
      val serializer    = extension.serializerFor(classOf[BankAccountDeposited])
      val now           = Clock.now
      val expectedEvent = BankAccountDeposited(BankAccountId(1L), Money.yens(100), now)
      val byteArray     = serializer.toBinary(expectedEvent)
      val event         = serializer.fromBinary(byteArray, Some(classOf[BankAccountDeposited]))
      event shouldBe expectedEvent
    }
    "should encode WithdrawEvent" in {
      val serializer    = extension.serializerFor(classOf[BankAccountWithdrawn])
      val now           = Clock.now
      val expectedEvent = BankAccountWithdrawn(BankAccountId(1L), Money.yens(100), now)
      val byteArray     = serializer.toBinary(expectedEvent)
      val event         = serializer.fromBinary(byteArray, Some(classOf[BankAccountWithdrawn]))
      event shouldBe expectedEvent
    }
    "should encode DestroyEvent" in {
      val serializer    = extension.serializerFor(classOf[BankAccountClosed])
      val now           = Clock.now
      val expectedEvent = BankAccountClosed(BankAccountId(1L), now)
      val byteArray     = serializer.toBinary(expectedEvent)
      val event         = serializer.fromBinary(byteArray, Some(classOf[BankAccountClosed]))
      event shouldBe expectedEvent
    }
  }
}
