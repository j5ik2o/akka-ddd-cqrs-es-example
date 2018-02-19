package com.github.j5ik2o.bank.infrastrucuture.akka.dsl

import akka.NotUsed
import akka.actor.{ ActorRef, ActorSystem }
import akka.stream.scaladsl.Flow
import com.github.j5ik2o.bank.infrastrucuture.akka.stage.ActorFlowStage

object ActorFlow {

  def apply[A, B](targetRef: ActorRef)(implicit system: ActorSystem): Flow[A, B, NotUsed] =
    Flow.fromGraph(new ActorFlowStage[A, B](targetRef))
}
