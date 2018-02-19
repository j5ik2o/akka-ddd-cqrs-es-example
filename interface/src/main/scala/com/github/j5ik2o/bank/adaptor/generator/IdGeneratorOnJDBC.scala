package com.github.j5ik2o.bank.adaptor.generator

import com.github.j5ik2o.bank.domain.model.{ BankAccountEventId, BankAccountId }
import slick.jdbc.JdbcProfile

import scala.concurrent.{ ExecutionContext, Future }

abstract class AbstractIdGeneratorOnJDBC[ID](val profile: JdbcProfile, val db: JdbcProfile#Backend#Database)
    extends IdGenerator[ID] {
  import profile.api._

  val tableName: String

  protected def internalGenerateId()(
      implicit ec: ExecutionContext
  ): Future[Long] = {
    val action = for {
      updateResult <- sqlu"UPDATE #${tableName} SET id = LAST_INSERT_ID(id+1)"
      _            <- if (updateResult == 1) DBIO.successful(Some(updateResult)) else DBIO.successful(None)
      selectResult <- sql"SELECT LAST_INSERT_ID() AS id".as[Long].headOption
    } yield selectResult
    db.run(action.transactionally).flatMap {
      case Some(id) => Future.successful(id)
      case None     => Future.failed(new Exception("Occurred id generation error"))
    }
  }
}

class BankAccountEventIdGeneratorOnJDBC(profile: JdbcProfile, db: JdbcProfile#Backend#Database)
    extends AbstractIdGeneratorOnJDBC[BankAccountEventId](profile, db) {
  override val tableName: String = "bank_account_event_id_sequence_number"

  override def generateId()(implicit ec: ExecutionContext): Future[BankAccountEventId] =
    internalGenerateId().map(BankAccountEventId)
}

class BankAccountIdGeneratorOnJDBC(profile: JdbcProfile, db: JdbcProfile#Backend#Database)
    extends AbstractIdGeneratorOnJDBC[BankAccountId](profile, db) {
  override val tableName: String = "bank_account_id_sequence_number"

  override def generateId()(implicit ec: ExecutionContext): Future[BankAccountId] =
    internalGenerateId().map(BankAccountId)
}
