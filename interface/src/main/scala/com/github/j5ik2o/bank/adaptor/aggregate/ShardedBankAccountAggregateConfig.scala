package com.github.j5ik2o.bank.adaptor.aggregate

import scala.concurrent.duration.Duration

case class ShardedBankAccountAggregateConfig(receiveTimeout: Duration)
