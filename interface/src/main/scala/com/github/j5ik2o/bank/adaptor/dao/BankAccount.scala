package com.github.j5ik2o.bank.adaptor.dao

trait BankAccountComponent extends ComponentSupport with BankAccountComponentSupport {

  import profile.api._

  case class BankAccountRecord(
      id: Long,
      deleted: Boolean,
      name: String,
      sequenceNr: Long,
      createdAt: java.time.ZonedDateTime,
      updatedAt: java.time.ZonedDateTime
  ) extends Record

  case class BankAccounts(tag: Tag) extends TableBase[BankAccountRecord](tag, "bank_account") {
    // def id = column[Long]("id", O.PrimaryKey)
    def deleted    = column[Boolean]("deleted")
    def name       = column[String]("name")
    def sequenceNr = column[Long]("sequence_nr")
    def createdAt  = column[java.time.ZonedDateTime]("created_at")
    def updatedAt  = column[java.time.ZonedDateTime]("updated_at")
    override def * =
      (id, deleted, name, sequenceNr, createdAt, updatedAt) <> (BankAccountRecord.tupled, BankAccountRecord.unapply)
  }

  object BankAccountDao
      extends TableQuery(BankAccounts)
      with DaoSupport[Long, BankAccountRecord]
      with BankAccountDaoSupport

}
