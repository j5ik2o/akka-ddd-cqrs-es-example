package com.github.j5ik2o.bank.adaptor.util

import akka.actor.ActorSystem
import com.github.j5ik2o.bank.adaptor.generator.IdGenerator
import com.github.j5ik2o.bank.domain.model.{ BankAccountId, BankAccountName }
import org.scalacheck.Gen
import org.sisioh.baseunits.scala.money.Money
import slick.basic.DatabaseConfig
import slick.jdbc.JdbcProfile

trait BankAccountSpecSupport {

  val system: ActorSystem

  lazy val dbConfig: DatabaseConfig[JdbcProfile] =
    DatabaseConfig.forConfig[JdbcProfile](path = "slick", system.settings.config)

  lazy val bankAccountIdGenerator: IdGenerator[BankAccountId] =
    IdGenerator.ofBankAccountId(dbConfig.profile, dbConfig.db)

  val bankAccountNameGen: Gen[BankAccountName] =
    Gen.alphaStr.suchThat(v => v.nonEmpty && v.length <= 256).map(BankAccountName)
  val depositMoneyGen: Gen[Money]  = Gen.choose(1L, 100L).map(v => Money.yens(BigDecimal(v)))
  val withdrawMoneyGen: Gen[Money] = Gen.choose(1L, 50L).map(v => Money.yens(BigDecimal(v)))

  val bankAccountOldNameAndNewNameGen: Gen[(BankAccountName, BankAccountName)] = for {
    oldName <- bankAccountNameGen
    newName <- bankAccountNameGen
  } yield (oldName, newName)

  val bankAccountNameAndDepositMoneyGen: Gen[(BankAccountName, Money)] = for {
    name    <- bankAccountNameGen
    deposit <- depositMoneyGen
  } yield (name, deposit)

  val bankAccountNameAndDepositMoneyAndWithDrawMoneyGen: Gen[(BankAccountName, Money, Money)] = (for {
    name     <- bankAccountNameGen
    deposit  <- depositMoneyGen
    withdraw <- withdrawMoneyGen
  } yield (name, deposit, withdraw)).suchThat { case (_, deposit, withdraw) => deposit > withdraw }

}
