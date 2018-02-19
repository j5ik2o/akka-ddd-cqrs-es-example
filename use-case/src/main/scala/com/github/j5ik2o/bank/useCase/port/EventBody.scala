package com.github.j5ik2o.bank.useCase.port

case class EventBody(persistenceId: String, sequenceNr: Long, event: Any)
