package com.github.j5ik2o.bank.infrastrucuture.akka.dsl

import akka.Done
import akka.actor.{ ActorRef, ActorSystem }
import akka.stream.scaladsl.{ Keep, Sink }

import scala.concurrent.Future

object ActorSink {

  def apply[A](targetRef: ActorRef)(implicit system: ActorSystem): Sink[Any, Future[Done]] =
    ActorFlow(targetRef).toMat(Sink.ignore)(Keep.right)

}
