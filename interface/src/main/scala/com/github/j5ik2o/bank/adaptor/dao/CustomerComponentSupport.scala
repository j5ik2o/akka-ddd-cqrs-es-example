package com.github.j5ik2o.bank.adaptor.dao

trait CustomerComponentSupport { this: CustomerComponent =>

  trait CustomerDaoSupport { this: DaoSupport[Long, CustomerRecord] =>

  }

}
