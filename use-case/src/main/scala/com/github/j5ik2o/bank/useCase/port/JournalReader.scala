package com.github.j5ik2o.bank.useCase.port

import akka.NotUsed
import akka.stream.scaladsl.Source

trait JournalReader {
  def eventsByTagSource(tag: String, seqNr: Long): Source[EventBody, NotUsed]
}
