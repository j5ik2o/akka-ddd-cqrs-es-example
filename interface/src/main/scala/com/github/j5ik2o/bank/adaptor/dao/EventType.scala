package com.github.j5ik2o.bank.adaptor.dao

import enumeratum.{ Enum, EnumEntry }

sealed abstract class EventType(override val entryName: String) extends EnumEntry

object EventType extends Enum[EventType] {
  override def values = findValues
  case object Deposit  extends EventType("deposit")
  case object Withdraw extends EventType("withdraw")
}
