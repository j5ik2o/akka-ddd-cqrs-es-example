package com.github.j5ik2o.bank.adaptor.dao

trait BankAccountEventComponent extends ComponentSupport with BankAccountEventComponentSupport {

  import profile.api._

  case class BankAccountEventRecord(
      id: Long,
      bankAccountId: Long,
      `type`: String,
      amount: Long,
      currencyCode: String,
      createdAt: java.time.ZonedDateTime
  ) extends Record

  case class BankAccountEvents(tag: Tag) extends TableBase[BankAccountEventRecord](tag, "bank_account_event") {
    // def id = column[Long]("id", O.PrimaryKey)
    def bankAccountId = column[Long]("bank_account_id")
    def `type`        = column[String]("type")
    def amount        = column[Long]("amount")
    def currencyCode  = column[String]("currency_code")
    def createdAt     = column[java.time.ZonedDateTime]("created_at")
    override def * =
      (id, bankAccountId, `type`, amount, currencyCode, createdAt) <> (BankAccountEventRecord.tupled, BankAccountEventRecord.unapply)
  }

  object BankAccountEventDao
      extends TableQuery(BankAccountEvents)
      with DaoSupport[Long, BankAccountEventRecord]
      with BankAccountEventDaoSupport

}
