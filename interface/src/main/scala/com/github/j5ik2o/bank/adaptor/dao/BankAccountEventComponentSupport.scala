package com.github.j5ik2o.bank.adaptor.dao

trait BankAccountEventComponentSupport { this: BankAccountEventComponent =>

  import profile.api._

  implicit val eventTypeColumnType = MappedColumnType.base[EventType, String](
    { _.entryName }, { s =>
      EventType.withName(s)
    }
  )

  trait BankAccountEventDaoSupport { this: DaoSupport[Long, BankAccountEventRecord] =>

  }

}
