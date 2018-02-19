package com.github.j5ik2o.bank.adaptor.aggregate

import akka.actor.{ ActorRef, ActorSystem }
import com.github.j5ik2o.bank.adaptor.util.{ ActorSpec, FlywayWithMySQLSpecSupport }
import com.typesafe.config.Config

abstract class PersistenceSpec(system: ActorSystem) extends ActorSpec(system) with FlywayWithMySQLSpecSupport {

  def this(name: String, config: Config) = this(ActorSystem(name, config))

  protected def killActors(actors: ActorRef*): Unit = {
    actors.foreach { actor =>
      watch(actor)
      system.stop(actor)
      expectTerminated(actor)
    }
  }

}
