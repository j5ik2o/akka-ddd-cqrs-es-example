package com.github.j5ik2o.bank.infrastrucuture.akka.protocol

import akka.actor.ActorRef

object ActorProtocol {

  case class Request(sender: ActorRef, msg: Any)

}
