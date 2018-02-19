package com.github.j5ik2o.bank.adaptor.useCase

import akka.actor.ActorSystem
import com.github.j5ik2o.bank.adaptor.aggregate.{ BankAccountAggregate, BankAccountAggregateFlowsImpl }
import com.github.j5ik2o.bank.adaptor.dao.BankAccountReadModelFlowsImpl
import com.github.j5ik2o.bank.adaptor.readJournal.JournalReaderImpl
import com.github.j5ik2o.bank.adaptor.util.{ ActorSpec, BankAccountSpecSupport, FlywayWithMySQLSpecSupport }
import com.github.j5ik2o.bank.domain.model.{ BankAccountId, BankAccountName }
import com.github.j5ik2o.bank.useCase.{ BankAccountAggregateUseCase, BankAccountReadModelUseCase }
import com.github.j5ik2o.bank.useCase.BankAccountAggregateUseCase.Protocol._
import com.github.j5ik2o.scalatestplus.db.{ MySQLdConfig, UserWithPassword }
import com.typesafe.config.ConfigFactory
import com.wix.mysql.distribution.Version.v5_6_21
import org.scalatest.time.{ Millis, Seconds, Span }
import org.sisioh.baseunits.scala.money.Money

import scala.concurrent.duration._

class BankAccountReadModelUseCaseImplSpec
    extends ActorSpec(
      ActorSystem("BankAccountReadModelUseCaseImplSpec", ConfigFactory.load("bank-account-use-case-spec.conf"))
    )
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

  "BankAccountReadModelUseCaseImpl" - {
    "should be able to read read-model" in {
      val id           = bankAccountIdGenerator.generateId().futureValue
      val aggregateRef = system.actorOf(BankAccountAggregate.props, BankAccountAggregate.name(id))
      val bankAccountReadModelUseCase = new BankAccountReadModelUseCase(
        new BankAccountReadModelFlowsImpl(dbConfig.profile, dbConfig.db),
        new JournalReaderImpl()
      )
      bankAccountReadModelUseCase.execute()
      createDomainEvents(id,
                         new BankAccountAggregateUseCase(
                           new BankAccountAggregateFlowsImpl(aggregateRef)
                         ))
      awaitAssert(
        {
          val resolveBankAccountEventsSucceeded = bankAccountReadModelUseCase
            .resolveBankAccountEventsById(ResolveBankAccountEventsRequest(id))
            .futureValue
            .asInstanceOf[ResolveBankAccountEventsSucceeded]
          resolveBankAccountEventsSucceeded.bankAccountId shouldBe id
          resolveBankAccountEventsSucceeded.events.head.`type` shouldBe "deposit"
          resolveBankAccountEventsSucceeded.events.head.amount shouldBe 1000
          resolveBankAccountEventsSucceeded.events.head.currencyCode shouldBe "JPY"
        },
        3 seconds,
        50 milliseconds
      )
    }
  }

  private def createDomainEvents(id: BankAccountId, bankAccountAggregateUseCase: BankAccountAggregateUseCase) = {
    val openBankAccountSucceeded = bankAccountAggregateUseCase
      .openBankAccount(OpenBankAccountRequest(id, BankAccountName("test-1")))
      .futureValue
      .asInstanceOf[OpenBankAccountSucceeded]
    openBankAccountSucceeded.bankAccountId shouldBe id
    val depositSucceeded =
      bankAccountAggregateUseCase
        .addBankAccountEvent(DepositRequest(id, Money.yens(1000L)))
        .futureValue
        .asInstanceOf[DepositSucceeded]
    depositSucceeded.bankAccountId shouldBe id
  }
}
