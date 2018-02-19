package com.github.j5ik2o.bank.adaptor.generator

import com.github.j5ik2o.bank.domain.model.{ BankAccountEventId, BankAccountId }
import slick.jdbc.JdbcProfile

import scala.concurrent.{ ExecutionContext, Future }

object IdGenerator {

  def ofBankAccountId(profile: JdbcProfile, db: JdbcProfile#Backend#Database): IdGenerator[BankAccountId] =
    new BankAccountIdGeneratorOnJDBC(profile, db)

  def ofBankAccountEventId(profile: JdbcProfile, db: JdbcProfile#Backend#Database): IdGenerator[BankAccountEventId] =
    new BankAccountEventIdGeneratorOnJDBC(profile, db)

}

trait IdGenerator[ID] {
  def generateId()(implicit ec: ExecutionContext): Future[ID]
}
