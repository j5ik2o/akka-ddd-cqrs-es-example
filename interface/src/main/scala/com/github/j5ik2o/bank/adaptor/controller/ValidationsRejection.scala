package com.github.j5ik2o.bank.adaptor.controller

import akka.http.javadsl.server.CustomRejection
import cats.data.NonEmptyList

case class ValidationsRejection(errors: NonEmptyList[Error]) extends CustomRejection
