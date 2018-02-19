package com.github.j5ik2o.bank.apiServer

import akka.actor.{ ActorRef, ActorSystem }
import akka.http.scaladsl.Http
import akka.stream.ActorMaterializer
import com.github.j5ik2o.bank.adaptor.aggregate.{ BankAccountAggregateFlowsImpl, ShardedBankAccountAggregates }
import com.github.j5ik2o.bank.adaptor.controller.Routes
import com.github.j5ik2o.bank.adaptor.dao.BankAccountReadModelFlowsImpl
import com.github.j5ik2o.bank.adaptor.generator.IdGenerator
import com.github.j5ik2o.bank.adaptor.readJournal.JournalReaderImpl
import com.github.j5ik2o.bank.useCase.{ BankAccountAggregateUseCase, BankAccountReadModelUseCase }
import com.typesafe.config.{ Config, ConfigFactory }
import pureconfig._
import slick.basic.DatabaseConfig
import slick.jdbc.JdbcProfile

import scala.concurrent.ExecutionContextExecutor

object Main extends App {
  val rootConfig: Config                    = ConfigFactory.load()
  val dbConfig: DatabaseConfig[JdbcProfile] = DatabaseConfig.forConfig[JdbcProfile](path = "slick", rootConfig)

  implicit val system: ActorSystem                        = ActorSystem("bank-system", config = rootConfig)
  implicit val materializer: ActorMaterializer            = ActorMaterializer()
  implicit val executionContext: ExecutionContextExecutor = system.dispatcher

  val bankAccountIdGenerator = IdGenerator.ofBankAccountId(dbConfig.profile, dbConfig.db)

  val bankAccountAggregatesRef: ActorRef =
    system.actorOf(ShardedBankAccountAggregates.props, ShardedBankAccountAggregates.name)

  val bankAccountAggregateUseCase: BankAccountAggregateUseCase = new BankAccountAggregateUseCase(
    new BankAccountAggregateFlowsImpl(bankAccountAggregatesRef)
  )

  val bankAccountReadModelUseCase: BankAccountReadModelUseCase =
    new BankAccountReadModelUseCase(new BankAccountReadModelFlowsImpl(dbConfig.profile, dbConfig.db),
                                    new JournalReaderImpl())

  val routes: Routes = Routes(bankAccountIdGenerator, bankAccountAggregateUseCase, bankAccountReadModelUseCase)

  val ApiServerConfig(host, port) =
    loadConfigOrThrow[ApiServerConfig](system.settings.config.getConfig("bank.api-server"))

  val bindingFuture = Http().bindAndHandle(routes.root, host, port)

  sys.addShutdownHook {
    bindingFuture
      .flatMap(_.unbind())
      .onComplete(_ => system.terminate())
  }
}
