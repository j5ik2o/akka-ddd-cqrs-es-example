package com.github.j5ik2o.bank.adaptor.dao

trait BankAccountComponentSupport { this: BankAccountComponent =>

  trait BankAccountDaoSupport { this: DaoSupport[Long, BankAccountRecord] =>

  }

}
