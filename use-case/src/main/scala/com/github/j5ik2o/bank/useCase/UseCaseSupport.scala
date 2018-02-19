package com.github.j5ik2o.bank.useCase

import akka.{ Done, NotUsed }
import akka.stream.{ FlowShape, QueueOfferResult }
import akka.stream.scaladsl.{ Flow, GraphDSL, Sink, SourceQueueWithComplete, Unzip, Zip }

import scala.concurrent.{ ExecutionContext, Future, Promise }

object UseCaseSupport {
  implicit class FlowOps[A, B](val self: Flow[A, B, NotUsed]) extends AnyVal {
    def zipPromise: Flow[(A, Promise[B]), (B, Promise[B]), NotUsed] =
      Flow
        .fromGraph(GraphDSL.create() { implicit b =>
          import GraphDSL.Implicits._
          val unzip = b.add(Unzip[A, Promise[B]])
          val zip   = b.add(Zip[B, Promise[B]])
          unzip.out0 ~> self ~> zip.in0
          unzip.out1 ~> zip.in1
          FlowShape(unzip.in, zip.out)
        })
  }
}

trait UseCaseSupport {

  protected def offerToQueue[A, B](
      sourceQueue: SourceQueueWithComplete[(A, Promise[B])]
  )(request: A, promise: Promise[B])(implicit ec: ExecutionContext): Future[B] = {
    sourceQueue.offer((request, promise)).flatMap {
      case QueueOfferResult.Enqueued =>
        promise.future
      case QueueOfferResult.Failure(t) =>
        Future.failed(new Exception("Failed to offer request", t))
      case QueueOfferResult.Dropped =>
        Future.failed(
          new Exception(
            s"Failed to enqueue resolve request, the queue buffer was full, please check the bank.interface.buffer-size setting"
          )
        )
      case QueueOfferResult.QueueClosed =>
        Future.failed(new Exception("Failed to enqueue request batch write, the queue was closed"))
    }
  }

  protected def completePromiseSink[T]: Sink[(T, Promise[T]), Future[Done]] = Sink.foreach {
    case (response, promise) =>
      promise.success(response)
  }

}
