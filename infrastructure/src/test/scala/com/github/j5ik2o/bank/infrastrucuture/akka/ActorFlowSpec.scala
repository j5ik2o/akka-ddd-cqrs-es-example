package com.github.j5ik2o.bank.infrastrucuture.akka

import akka.actor.{ ActorSystem, Props }
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.Source
import akka.stream.testkit.scaladsl.TestSink
import akka.testkit.TestKit
import com.github.j5ik2o.bank.infrastrucuture.akka.dsl.ActorFlow
import org.scalatest.FreeSpecLike

class ActorFlowSpec extends TestKit(ActorSystem("ActorFlowSpec")) with FreeSpecLike {

  implicit val mat = ActorMaterializer()

  "ActorFlow" - {
    "should be able to send message via stream" in {
      val props     = Props(FlowActor[String]({ case (subscriber, x) => subscriber ! x }))
      val flowActor = system.actorOf(props)
      val sinkProbe =
        Source.single("TEST").via(ActorFlow[String, String](flowActor)).runWith(TestSink.probe)
      sinkProbe.request(1).expectNext("TEST")
    }
    "should be able to error handling" in {
      val props     = Props(FlowActor[String]({ case (_, x) => throw new Exception("message = " + x) }))
      val flowActor = system.actorOf(props)
      val sinkProbe =
        Source.single("TEST").via(ActorFlow[String, String](flowActor)).runWith(TestSink.probe)
      sinkProbe.request(1).expectError()
    }
  }
}
