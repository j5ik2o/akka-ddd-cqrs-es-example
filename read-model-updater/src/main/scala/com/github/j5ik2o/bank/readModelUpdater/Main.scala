package com.github.j5ik2o.bank.readModelUpdater

import akka.actor.ActorSystem
import com.github.j5ik2o.bank.adaptor.dao.BankAccountReadModelFlowsImpl
import com.github.j5ik2o.bank.adaptor.readJournal.JournalReaderImpl
import com.github.j5ik2o.bank.useCase.BankAccountReadModelUseCase
import com.typesafe.config.ConfigFactory
import slick.basic.DatabaseConfig
import slick.jdbc.JdbcProfile

object Main extends App {
  val rootConfig      = ConfigFactory.load()
  implicit val system = ActorSystem("bank-system", config = rootConfig)
  val dbConfig        = DatabaseConfig.forConfig[JdbcProfile](path = "slick", rootConfig)

  new BankAccountReadModelUseCase(new BankAccountReadModelFlowsImpl(dbConfig.profile, dbConfig.db),
                                  new JournalReaderImpl())
    .execute()

  sys.addShutdownHook {
    system.terminate()
  }
}
