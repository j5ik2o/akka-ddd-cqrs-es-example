package com.github.j5ik2o.bank.adaptor.controller

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Directives.complete
import akka.http.scaladsl.server._
import com.github.j5ik2o.bank.adaptor.controller.Routes.ValidationErrorsResponseJson
import de.heikoseeberger.akkahttpcirce.FailFastCirceSupport._
import io.circe.generic.auto._

object RejectionHandlers {

  final val default: RejectionHandler = RejectionHandler
    .newBuilder()
    .handle {
      case ValidationsRejection(errors) =>
        complete((StatusCodes.BadRequest, ValidationErrorsResponseJson(errors.map(_.message).toList)))
    }
    .result()

}
