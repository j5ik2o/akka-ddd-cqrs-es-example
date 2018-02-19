package com.github.j5ik2o.bank.adaptor.dao

trait CustomerComponent extends ComponentSupport with CustomerComponentSupport {

  import profile.api._

  case class CustomerRecord(
      id: Long,
      name: String,
      createdAt: java.time.ZonedDateTime,
      updatedAt: java.time.ZonedDateTime
  ) extends Record

  case class Customers(tag: Tag) extends TableBase[CustomerRecord](tag, "customer") {
    // def id = column[Long]("id", O.PrimaryKey)
    def name       = column[String]("name")
    def createdAt  = column[java.time.ZonedDateTime]("created_at")
    def updatedAt  = column[java.time.ZonedDateTime]("updated_at")
    override def * = (id, name, createdAt, updatedAt) <> (CustomerRecord.tupled, CustomerRecord.unapply)
  }

  object CustomerDao extends TableQuery(Customers) with DaoSupport[Long, CustomerRecord] with CustomerDaoSupport

}
