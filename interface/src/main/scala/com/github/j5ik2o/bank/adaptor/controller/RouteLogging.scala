package com.github.j5ik2o.bank.adaptor.controller

import akka.event.Logging
import akka.http.scaladsl.model.headers.{ Authorization, BasicHttpCredentials }
import akka.http.scaladsl.model.{ HttpRequest, HttpResponse }
import akka.http.scaladsl.server.{ Directive0, RouteResult }
import akka.http.scaladsl.server.directives.{ DebuggingDirectives, LogEntry }

trait RequestFormatter {
  def formatRequest(request: HttpRequest): LogEntry
}

trait RequestResultFormatter {
  def formatRequestResponse(request: HttpRequest): RouteResult => Option[LogEntry]
}

object DefaultRequestLoggingFormatter {

  implicit object requestFormatter extends RequestFormatter {
    override def formatRequest(request: HttpRequest): LogEntry = {
      LogEntry(s"${formatRequestToString(request)}/", Logging.InfoLevel)
    }
  }

  implicit object requestResultFormatter extends RequestResultFormatter {
    override def formatRequestResponse(request: HttpRequest): (RouteResult) => Option[LogEntry] = {
      case RouteResult.Complete(response) =>
        val req = formatRequestToString(request)
        val res = formatResponseToString(response)
        Some(LogEntry(s"$req/$res", Logging.InfoLevel))
      case _ =>
        val req = formatRequestToString(request)
        Some(LogEntry(req, Logging.InfoLevel))
    }
  }

  private def formatRequestToString(request: HttpRequest): String = {
    val protocol = request.protocol.value
    val method   = request.method.name()
    val path     = request.uri.toString()
    val headers = request.headers
      .collect {
        case Authorization(_: BasicHttpCredentials) => "authorization:Basic ***"
        case Authorization(_)                       => "authorization:***"
        case h                                      => s"'${h.lowercaseName()}':'${h.value()}'"
      }
      .mkString(", ")
    s"$protocol $method $path [$headers]"
  }

  private def formatResponseToString(request: HttpResponse): String = {
    val status  = request.status.value
    val headers = request.headers.map(h => s"'${h.lowercaseName()}':'${h.value()}'").mkString(", ")
    s"$status [$headers]"
  }
}

class RouteLogging()(implicit requestFormatter: RequestFormatter, requestResultFormatter: RequestResultFormatter) {

  val httpLogRequest: Directive0 = DebuggingDirectives.logRequest(requestFormatter.formatRequest _)

  val httpLogRequestResult: Directive0 =
    DebuggingDirectives.logRequestResult(requestResultFormatter.formatRequestResponse _)

}

object RouteLogging {
  def apply()(implicit requestFormatter: RequestFormatter,
              requestResultFormatter: RequestResultFormatter): RouteLogging = new RouteLogging()

  val default: RouteLogging = new RouteLogging()(DefaultRequestLoggingFormatter.requestFormatter,
                                                 DefaultRequestLoggingFormatter.requestResultFormatter)
}
