package com.github.j5ik2o.bank.adaptor.controller

import akka.http.scaladsl.model.StatusCodes
import com.github.j5ik2o.bank.adaptor.aggregate.{ BankAccountAggregateFlowsImpl, LocalBankAccountAggregates }
import com.github.j5ik2o.bank.adaptor.controller.Routes.{
  AddBankAccountEventRequestJson,
  AddBankAccountEventResponseJson,
  CloseBankAccountResponseJson,
  OpenBankAccountRequestJson,
  OpenBankAccountResponseJson,
  ResolveBankAccountEventsResponseJson,
  UpdateBankAccountRequestJson,
  UpdateBankAccountResponseJson
}
import com.github.j5ik2o.bank.adaptor.dao.{ BankAccountReadModelFlowsImpl, EventType }
import com.github.j5ik2o.bank.adaptor.generator.IdGenerator
import com.github.j5ik2o.bank.adaptor.readJournal.JournalReaderImpl
import com.github.j5ik2o.bank.adaptor.util.ControllerSpec
import com.github.j5ik2o.bank.domain.model.BankAccountId
import com.github.j5ik2o.bank.useCase.{ BankAccountAggregateUseCase, BankAccountReadModelUseCase }
import com.typesafe.config.{ Config, ConfigFactory }
import de.heikoseeberger.akkahttpcirce.FailFastCirceSupport._
import io.circe.generic.auto._
import slick.basic.DatabaseConfig
import slick.jdbc.JdbcProfile

import scala.concurrent.duration._

class BankAccountControllerSpec extends ControllerSpec with BankAccountController {

  import ControllerSpec._

  override def testConfig: Config = ConfigFactory.load("bank-account-use-case-spec.conf")

  val dbConfig = DatabaseConfig.forConfig[JdbcProfile](path = "slick", testConfig)

  override protected val bankAccountIdGenerator: IdGenerator[BankAccountId] =
    IdGenerator.ofBankAccountId(dbConfig.profile, dbConfig.db)

  private val aggregatesRef = system.actorOf(LocalBankAccountAggregates.props, LocalBankAccountAggregates.name)

  override val bankAccountAggregateUseCase: BankAccountAggregateUseCase = new BankAccountAggregateUseCase(
    new BankAccountAggregateFlowsImpl(aggregatesRef)
  )

  override val bankAccountReadModelUseCase: BankAccountReadModelUseCase = {
    val bankAccountReadModelUseCase = new BankAccountReadModelUseCase(
      new BankAccountReadModelFlowsImpl(dbConfig.profile, dbConfig.db),
      new JournalReaderImpl()
    )
    bankAccountReadModelUseCase.execute()
    bankAccountReadModelUseCase
  }

  "BankAccountController" - {
    "should open BankAccount" in {
      Post("/bank-accounts").withEntity(OpenBankAccountRequestJson(name = "test").toEntity) ~> openBankAccount ~> check {
        status shouldEqual StatusCodes.OK
        val openBankAccountResponseJson = responseAs[OpenBankAccountResponseJson]
        openBankAccountResponseJson.isSuccessful shouldBe true
      }
    }
    "should update BankAccount" in {
      Post("/bank-accounts").withEntity(OpenBankAccountRequestJson(name = "test").toEntity) ~> openBankAccount ~> check {
        status shouldEqual StatusCodes.OK
        val openBankAccountResponseJson = responseAs[OpenBankAccountResponseJson]
        Put(s"/bank-accounts/${openBankAccountResponseJson.id}").withEntity(
          UpdateBankAccountRequestJson(name = "test-2").toEntity
        ) ~> updateBankAccount ~> check {
          status shouldEqual StatusCodes.OK
          val updateBankAccountResponseJson = responseAs[UpdateBankAccountResponseJson]
          updateBankAccountResponseJson.isSuccessful shouldBe true
          updateBankAccountResponseJson.id shouldBe openBankAccountResponseJson.id
        }
      }
    }
    "should add BankAccountEvent" in {
      Post("/bank-accounts").withEntity(OpenBankAccountRequestJson(name = "test").toEntity) ~> openBankAccount ~> check {
        status shouldEqual StatusCodes.OK
        val openBankAccountResponseJson = responseAs[OpenBankAccountResponseJson]
        Put(s"/bank-accounts/${openBankAccountResponseJson.id}/events").withEntity(
          AddBankAccountEventRequestJson(`type` = EventType.Deposit.entryName, amount = 1000L, currencyCode = "JPY").toEntity
        ) ~> addBankAccountEvent ~> check {
          status shouldEqual StatusCodes.OK
          val addBankAccountEventResponseJson = responseAs[AddBankAccountEventResponseJson]
          addBankAccountEventResponseJson.isSuccessful shouldBe true
          addBankAccountEventResponseJson.id shouldBe openBankAccountResponseJson.id
        }
      }
    }
    "should close BankAccount" in {
      Post("/bank-accounts").withEntity(OpenBankAccountRequestJson(name = "test").toEntity) ~> openBankAccount ~> check {
        status shouldEqual StatusCodes.OK
        val openBankAccountResponseJson = responseAs[OpenBankAccountResponseJson]
        openBankAccountResponseJson.isSuccessful shouldBe true
        Delete(s"/bank-accounts/${openBankAccountResponseJson.id}") ~> closeBankAccount ~> check {
          status shouldEqual StatusCodes.OK
          val closeBankAccountResponseJson = responseAs[CloseBankAccountResponseJson]
          closeBankAccountResponseJson.isSuccessful shouldBe true
          closeBankAccountResponseJson.id shouldBe openBankAccountResponseJson.id
        }
      }
    }
    "should read BankAccount read-model" ignore {
      Post("/bank-accounts").withEntity(OpenBankAccountRequestJson(name = "test").toEntity) ~> openBankAccount ~> check {
        status shouldEqual StatusCodes.OK
        val openBankAccountResponseJson = responseAs[OpenBankAccountResponseJson]
        Put(s"/bank-accounts/${openBankAccountResponseJson.id}/events").withEntity(
          AddBankAccountEventRequestJson(`type` = EventType.Deposit.entryName, amount = 1000L, currencyCode = "JPY").toEntity
        ) ~> addBankAccountEvent ~> check {
          status shouldEqual StatusCodes.OK
          val addBankAccountEventResponseJson = responseAs[AddBankAccountEventResponseJson]
          addBankAccountEventResponseJson.isSuccessful shouldBe true
          addBankAccountEventResponseJson.id shouldBe openBankAccountResponseJson.id
          Thread.sleep(10000)
          awaitAssert(
            {
              Get(s"/bank-accounts/${openBankAccountResponseJson.id}") ~> resolveBankAccountEventsById ~> check {
                status shouldEqual StatusCodes.OK
                val resolveBankAccountEventsSucceeded = responseAs[ResolveBankAccountEventsResponseJson]
                resolveBankAccountEventsSucceeded.isSuccessful shouldBe true
                resolveBankAccountEventsSucceeded.id shouldBe openBankAccountResponseJson.id
                resolveBankAccountEventsSucceeded.values.nonEmpty shouldBe true
              }
            },
            3 seconds,
            50 milliseconds
          )
        }
      }

    }
  }
}
