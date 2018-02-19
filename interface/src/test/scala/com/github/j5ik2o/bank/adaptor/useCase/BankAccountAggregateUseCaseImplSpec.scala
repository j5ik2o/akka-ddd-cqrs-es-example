package com.github.j5ik2o.bank.adaptor.useCase

import akka.actor.ActorSystem
import com.github.j5ik2o.bank.adaptor.aggregate.{ BankAccountAggregate, BankAccountAggregateFlowsImpl }
import com.github.j5ik2o.bank.adaptor.util.{ ActorSpec, BankAccountSpecSupport, FlywayWithMySQLSpecSupport }
import com.github.j5ik2o.bank.useCase.BankAccountAggregateUseCase
import com.github.j5ik2o.bank.useCase.BankAccountAggregateUseCase.Protocol._
import com.github.j5ik2o.scalatestplus.db.{ MySQLdConfig, UserWithPassword }
import com.typesafe.config.ConfigFactory
import com.wix.mysql.distribution.Version.v5_6_21
import org.scalacheck.Shrink
import org.scalatest.time.{ Millis, Seconds, Span }
import org.sisioh.baseunits.scala.money.Money

import scala.concurrent.duration._

class BankAccountAggregateUseCaseImplSpec
    extends ActorSpec(ActorSystem("BankAccountUseCaseImplSpec", ConfigFactory.load("bank-account-use-case-spec.conf")))
    with FlywayWithMySQLSpecSupport
    with BankAccountSpecSupport {

  override implicit val patienceConfig: PatienceConfig =
    PatienceConfig(timeout = Span(5, Seconds), interval = Span(200, Millis))

  override protected lazy val mySQLdConfig: MySQLdConfig = MySQLdConfig(
    version = v5_6_21,
    port = Some(12345),
    userWithPassword = Some(UserWithPassword("bank", "passwd")),
    timeout = Some((30 seconds) * sys.env.getOrElse("SBT_TEST_TIME_FACTOR", "1").toDouble)
  )

  import system.dispatcher

  "BankAccountUseCaseImpl" - {
    "should open BankAccount" in {
      implicit val noShrink: Shrink[String] = Shrink.shrinkAny
      forAll(bankAccountNameGen) { bankAccountName =>
        val id           = bankAccountIdGenerator.generateId().futureValue
        val aggregateRef = system.actorOf(BankAccountAggregate.props, BankAccountAggregate.name(id))
        val bankAccountAggregateUseCase =
          new BankAccountAggregateUseCase(
            new BankAccountAggregateFlowsImpl(aggregateRef)
          )
        val openBankAccountSucceeded = bankAccountAggregateUseCase
          .openBankAccount(OpenBankAccountRequest(id, bankAccountName))
          .futureValue
          .asInstanceOf[OpenBankAccountSucceeded]
        openBankAccountSucceeded.bankAccountId shouldBe id
      }
    }
    "should update BankAccount" in {
      implicit val noShrink: Shrink[String] = Shrink.shrinkAny
      forAll(bankAccountOldNameAndNewNameGen) {
        case (oldName, newName) =>
          val id           = bankAccountIdGenerator.generateId().futureValue
          val aggregateRef = system.actorOf(BankAccountAggregate.props, BankAccountAggregate.name(id))
          val bankAccountAggregateUseCase =
            new BankAccountAggregateUseCase(
              new BankAccountAggregateFlowsImpl(aggregateRef)
            )
          val openBankAccountSucceeded = bankAccountAggregateUseCase
            .openBankAccount(OpenBankAccountRequest(id, oldName))
            .futureValue
            .asInstanceOf[OpenBankAccountSucceeded]
          openBankAccountSucceeded.bankAccountId shouldBe id
          val updateBankAccountSucceeded = bankAccountAggregateUseCase
            .updateBankAccount(UpdateBankAccountRequest(id, newName))
            .futureValue
            .asInstanceOf[UpdateBankAccountSucceeded]
          updateBankAccountSucceeded.bankAccountId shouldBe id
      }
    }
    "should deposit to BankAccount" in {
      implicit val noShrink: Shrink[(String, Money)] = Shrink.shrinkAny
      forAll(bankAccountNameAndDepositMoneyGen) {
        case (bankAccountName, depositMoney) =>
          val id           = bankAccountIdGenerator.generateId().futureValue
          val aggregateRef = system.actorOf(BankAccountAggregate.props, BankAccountAggregate.name(id))
          val bankAccountAggregateUseCase =
            new BankAccountAggregateUseCase(
              new BankAccountAggregateFlowsImpl(aggregateRef)
            )
          val openBankAccountSucceeded = bankAccountAggregateUseCase
            .openBankAccount(OpenBankAccountRequest(id, bankAccountName))
            .futureValue
            .asInstanceOf[OpenBankAccountSucceeded]
          openBankAccountSucceeded.bankAccountId shouldBe id
          val depositSucceeded =
            bankAccountAggregateUseCase
              .addBankAccountEvent(DepositRequest(id, depositMoney))
              .futureValue
              .asInstanceOf[DepositSucceeded]
          depositSucceeded.bankAccountId shouldBe id
      }
    }
    "should withdraw from BankAccount" in {
      implicit val noShrink: Shrink[(String, Money, Money)] = Shrink.shrinkAny
      forAll(bankAccountNameAndDepositMoneyAndWithDrawMoneyGen) {
        case (bankAccountName, depositMoney, withdrawMoney) =>
          val id           = bankAccountIdGenerator.generateId().futureValue
          val aggregateRef = system.actorOf(BankAccountAggregate.props, BankAccountAggregate.name(id))
          val bankAccountAggregateUseCase =
            new BankAccountAggregateUseCase(
              new BankAccountAggregateFlowsImpl(aggregateRef)
            )
          val openBankAccountSucceeded = bankAccountAggregateUseCase
            .openBankAccount(OpenBankAccountRequest(id, bankAccountName))
            .futureValue
            .asInstanceOf[OpenBankAccountSucceeded]
          openBankAccountSucceeded.bankAccountId shouldBe id
          val depositSucceeded =
            bankAccountAggregateUseCase
              .addBankAccountEvent(DepositRequest(id, depositMoney))
              .futureValue
              .asInstanceOf[DepositSucceeded]
          depositSucceeded.bankAccountId shouldBe id
          val withdrawSucceeded =
            bankAccountAggregateUseCase
              .addBankAccountEvent(WithdrawRequest(id, withdrawMoney))
              .futureValue
              .asInstanceOf[WithdrawSucceeded]
          withdrawSucceeded.bankAccountId shouldBe id
      }
    }
    "should close BankAccount" in {
      implicit val noShrink: Shrink[String] = Shrink.shrinkAny
      forAll(bankAccountNameGen) { bankAccountName =>
        val id           = bankAccountIdGenerator.generateId().futureValue
        val aggregateRef = system.actorOf(BankAccountAggregate.props, BankAccountAggregate.name(id))
        val bankAccountAggregateUseCase =
          new BankAccountAggregateUseCase(
            new BankAccountAggregateFlowsImpl(aggregateRef)
          )
        val openBankAccountSucceeded = bankAccountAggregateUseCase
          .openBankAccount(OpenBankAccountRequest(id, bankAccountName))
          .futureValue
          .asInstanceOf[OpenBankAccountSucceeded]
        openBankAccountSucceeded.bankAccountId shouldBe id
        val closeBankAccountSucceeded = bankAccountAggregateUseCase
          .closeBankAccount(CloseBankAccountRequest(id))
          .futureValue
          .asInstanceOf[CloseBankAccountSucceeded]
        closeBankAccountSucceeded.bankAccountId shouldBe id
      }
    }
  }

}
