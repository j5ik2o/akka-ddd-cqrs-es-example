package com.github.j5ik2o.bank.infrastrucuture.akka

import akka.actor.{ Actor, ActorLogging, ActorRef }
import com.github.j5ik2o.bank.infrastrucuture.akka.protocol.ActorProtocol.Request

case class SourceActor[A](f: (ActorRef, A) => Unit) extends Actor with ActorLogging {
  var subscriber: ActorRef = _

  override def receive: Receive = {
    case request: Request =>
      log.debug(s"SourceActor#message = $request")
      f(request.sender, request.msg.asInstanceOf[A])
  }
}
