package com.github.j5ik2o.bank.adaptor.serialization

import akka.persistence.journal.{ Tagged, WriteEventAdapter }
import com.github.j5ik2o.bank.domain.model.BankAccountEvent

class BankAccountEventAdaptor extends WriteEventAdapter {

  private def withTag(event: Any, tag: String) = Tagged(event, Set(tag))

  private val tagName = classOf[BankAccountEvent].getName

  override def manifest(event: Any): String = ""

  override def toJournal(event: Any): Any = {
    withTag(event, tagName)
  }

}
