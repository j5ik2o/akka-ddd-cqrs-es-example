package com.github.j5ik2o.bank.adaptor.aggregate

import scala.concurrent.duration.FiniteDuration

case class BankAccountAggregateFlowsConfig(callTimeout: FiniteDuration)
