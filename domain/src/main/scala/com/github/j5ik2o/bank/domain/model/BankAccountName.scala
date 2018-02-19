package com.github.j5ik2o.bank.domain.model

case class BankAccountName(value: String) {
  require(value.length < 255)
}
