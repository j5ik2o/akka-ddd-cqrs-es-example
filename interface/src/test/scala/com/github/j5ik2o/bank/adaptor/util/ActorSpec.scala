package com.github.j5ik2o.bank.adaptor.util

import akka.actor.ActorSystem
import akka.testkit.{ ImplicitSender, TestKit }
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.prop.PropertyChecks
import org.scalatest.{ BeforeAndAfterAll, FreeSpecLike, Matchers }

abstract class ActorSpec(system: ActorSystem)
    extends TestKit(system)
    with FreeSpecLike
    with PropertyChecks
    with ImplicitSender
    with Matchers
    with BeforeAndAfterAll
    with ScalaFutures {

  override def afterAll: Unit = TestKit.shutdownActorSystem(system)

}
