package com.github.j5ik2o.bank.infrastrucuture.akka.dsl

import akka.actor.{ ActorRef, ActorSystem, Props }
import akka.stream.scaladsl.Source
import com.github.j5ik2o.bank.infrastrucuture.akka.stage.ActorSourceStage

import scala.concurrent.Future

object ActorSource {

  def apply[A](props: Props)(implicit system: ActorSystem): Source[A, Future[ActorRef]] =
    Source.fromGraph(new ActorSourceStage[A](props))

}
