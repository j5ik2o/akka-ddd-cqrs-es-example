package com.github.j5ik2o.bank.infrastrucuture.akka.stage

import akka.actor.{ ActorRef, ActorSystem, Terminated }
import akka.stream.stage.GraphStageLogic.StageActor
import akka.stream.stage._
import akka.stream.{ Attributes, FlowShape, Inlet, Outlet }
import com.github.j5ik2o.bank.infrastrucuture.akka.protocol.ActorProtocol.Request

import scala.collection.mutable
import scala.util.{ Failure, Success, Try }

class ActorFlowStage[A, B](targetRef: ActorRef)(implicit system: ActorSystem) extends GraphStage[FlowShape[A, B]] {

  private val in = Inlet[A]("ActorFlowStage.int")

  private val out = Outlet[B]("ActorFlowStage.out")

  override def shape: FlowShape[A, B] = FlowShape(in, out)

  override def createLogic(inheritedAttributes: Attributes): GraphStageLogic =
    new GraphStageLogic(shape) with StageLogging {
      private var _self: StageActor                  = _
      private val requests: mutable.Queue[A]         = mutable.Queue.empty
      private val responses: mutable.Queue[B]        = mutable.Queue.empty
      private var inFlight: Int                      = _
      private var completionState: Option[Try[Unit]] = _

      private def tryToExecute(): Unit = {
        log.debug("tryToExecute: start")
        if (requests.nonEmpty) {
          inFlight += 1
          val message = requests.dequeue()
          log.debug(s"flowActor.tell: $message")
          targetRef.tell(Request(_self.ref, message), _self.ref)
        }
        if (responses.nonEmpty && isAvailable(out)) {
          inFlight -= 1
          val message = responses.dequeue()
          log.debug(s"push(out, $message)")
          push(out, message)
        }
        log.debug("tryToExecute: finish")
      }

      private def checkForCompletion(): Unit = {
        log.debug("checkForCompletion: start")
        val closedIn = isClosed(in)
        log.debug(s"inFlight = $inFlight, requests.isEmpty = ${requests.isEmpty}, isClosed(in) = $closedIn")
        if (inFlight == 0 && requests.isEmpty && closedIn) {
          completionState match {
            case Some(Success(_)) =>
              log.debug("checkForCompletion:completeStage")
              completeStage()
            case Some(Failure(ex)) =>
              log.debug(s"checkForCompletion:failStage($ex)")
              failStage(ex)
            case None =>
              log.debug(s"checkForCompletion:failStage(IllegalStateException)")
              failStage(new IllegalStateException("Stage completed, but there is no info about status"))
          }
        }
        log.debug("checkForCompletion: finish")
      }

      override def preStart(): Unit = {
        log.debug("preStart: start")
        super.preStart()
        inFlight = 0
        _self = getStageActor {
          case (_, ex: Exception) =>
            completionState = Some(Failure(ex))
            inFlight = 0
            checkForCompletion()
          case (_, Terminated(ref)) if targetRef == ref =>
            log.debug(s"receive message = Terminated($ref); failStage(...)")
            completionState = Some(Failure(new Exception("terminate actor")))
            inFlight = 0
            checkForCompletion()
          case (_, response) =>
            log.debug(s"receive response = $response")
            responses.enqueue(response.asInstanceOf[B])
            tryToExecute()
        }
        _self.watch(targetRef)
        log.debug("preStart: finish")
      }

      override def postStop(): Unit = {
        log.debug("postStop: start")
        super.postStop()
        inFlight = 0
        log.debug("postStop: finish")
      }

      setHandler(
        in,
        new InHandler {
          override def onUpstreamFinish(): Unit = {
            completionState = Some(Success(()))
            checkForCompletion()
          }

          override def onUpstreamFailure(ex: Throwable): Unit = {
            completionState = Some(Failure(ex))
            checkForCompletion()
          }

          override def onPush(): Unit = {
            log.debug("onPush: start")
            val message = grab(in)
            log.debug(s"onPush: $message")
            requests.enqueue(message)
            tryToExecute()
            log.debug("onPush: finish")
          }
        }
      )

      setHandler(
        out,
        new OutHandler {
          override def onPull(): Unit = {
            log.debug("onPull: start")
            if (!isClosed(in))
              pull(in)
            tryToExecute()
            log.debug("onPull: finish")
          }

          override def onDownstreamFinish(): Unit = {
            log.debug("onDownstreamFinish: start")
            if (!isClosed(out)) {
              log.debug(s"complete(out)")
              complete(out)
            }
            _self.become {
              case (_, ex: Exception) =>
                completionState = Some(Failure(ex))
                inFlight = 0
                checkForCompletion()
              case (_, Terminated(ref)) if ref == targetRef =>
                log.debug(s"receive message = Terminated($ref); completeStage()")
                completionState = Some(Failure(new Exception("terminate actor")))
                inFlight = 0
                checkForCompletion()
            }
            _self.unwatch(targetRef)
            log.debug("onDownstreamFinish: finish")
          }
        }
      )

    }

}
