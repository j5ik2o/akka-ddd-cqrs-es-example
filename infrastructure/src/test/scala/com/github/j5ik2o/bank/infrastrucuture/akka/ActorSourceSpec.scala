package com.github.j5ik2o.bank.infrastrucuture.akka

import akka.actor.{ ActorSystem, Props }
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.Keep
import akka.stream.testkit.scaladsl.TestSink
import akka.testkit.TestKit
import com.github.j5ik2o.bank.infrastrucuture.akka.dsl.ActorSource
import org.scalatest.FreeSpecLike
import org.scalatest.concurrent.ScalaFutures

class ActorSourceSpec extends TestKit(ActorSystem("ActorSourceSpec")) with FreeSpecLike with ScalaFutures {

  implicit val mat = ActorMaterializer()

  "ActorSource" - {
    "should be able to send message via stream" in {
      val props = Props(SourceActor[String]({ case (subscriber, msg) => subscriber ! msg }))

      val (sourceRefFuture, sinkProbe) = ActorSource[String](props).toMat(TestSink.probe)(Keep.both).run()

      sourceRefFuture.futureValue ! "TEST"
      sinkProbe.request(1).expectNext("TEST")
    }
    "should be able to error handling" in {
      val props = Props(SourceActor[String]({ case (_, x) => throw new Exception(s"message = $x") }))

      val (sourceRefFuture, sinkProbe) = ActorSource[String](props).toMat(TestSink.probe)(Keep.both).run()

      sourceRefFuture.futureValue ! "TEST"
      sinkProbe.request(1).expectError()
    }
  }
}
