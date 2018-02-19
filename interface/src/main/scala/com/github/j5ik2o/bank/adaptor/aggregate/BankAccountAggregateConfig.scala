package com.github.j5ik2o.bank.adaptor.aggregate

import scala.concurrent.duration.Duration

case class BankAccountAggregateConfig(receiveTimeout: Duration, numOfEventsToSnapshot: Int)
