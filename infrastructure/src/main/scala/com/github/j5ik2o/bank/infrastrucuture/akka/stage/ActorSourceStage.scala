package com.github.j5ik2o.bank.infrastrucuture.akka.stage

import akka.actor.SupervisorStrategy.Stop
import akka.actor.{ Actor, ActorRef, ActorSystem, OneForOneStrategy, Props, SupervisorStrategy, Terminated }
import akka.stream.stage.GraphStageLogic.StageActor
import akka.stream.stage._
import akka.stream.{ Attributes, Outlet, SourceShape }
import com.github.j5ik2o.bank.infrastrucuture.akka.protocol.ActorProtocol.Request

import scala.collection.immutable.Queue
import scala.concurrent.{ Future, Promise }
import scala.util.{ Failure, Success, Try }

class ActorSourceStage[A](props: Props)(implicit system: ActorSystem)
    extends GraphStageWithMaterializedValue[SourceShape[A], Future[ActorRef]] {

  private val out: Outlet[A] = Outlet("ActorSourceStage.out")

  override def shape: SourceShape[A] = SourceShape(out)

  override def createLogicAndMaterializedValue(inheritedAttributes: Attributes): (GraphStageLogic, Future[ActorRef]) = {
    val actorRefPromise = Promise[ActorRef]()
    val logic = new GraphStageLogic(shape) with StageLogging {
      private var buffer: Queue[A]                   = Queue.empty[A]
      private var _self: StageActor                  = _
      private var completionState: Option[Try[Unit]] = _
      private var inFlight: Int                      = _
      private var supervisor: ActorRef               = _

      class Supervisor extends Actor {
        private val childRef = context.actorOf(props)
        context.watch(childRef)

        override def supervisorStrategy: SupervisorStrategy = OneForOneStrategy() {
          case ex: Throwable =>
            _self.ref ! ex
            Stop
        }

        override def receive: Receive = {
          case Terminated(_childRef) if _childRef == childRef =>
            context.stop(self)
          case msg => childRef forward Request(_self.ref, msg)
        }
      }

      private def tryToProduce(): Unit = {
        log.info(s"queue = $buffer")
        if (buffer.nonEmpty && isAvailable(out)) {
          val (head, tail) = buffer.dequeue
          buffer = tail
          log.info(s"send message = $head")
          push(out, head)
          inFlight -= 1
        }
      }

      private def checkForCompletion(): Unit = {
        log.debug("checkForCompletion: start")
        log.debug(s"inFlight = $inFlight, buffer.isEmpty = ${buffer.isEmpty}")
        if (inFlight == 0 && buffer.isEmpty) {
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
        super.preStart()
        _self = getStageActor {
          case (sender, ex: Exception) =>
            log.error(ex, s"occurred error in source actor: $sender")
            completionState = Some(Failure(ex))
            inFlight = 0
            checkForCompletion()
          case (sender, Terminated(ref)) if supervisor == ref =>
            log.debug(s"receive message = Terminated($ref); failStage(...)")
            completionState = Some(Failure(new Exception("terminate actor")))
            inFlight = 0
            checkForCompletion()
          case (sender, message) =>
            log.debug(s"receive message = ($sender, $message)")
            inFlight += 1
            buffer = buffer.enqueue(message.asInstanceOf[A])
            tryToProduce()
        }
        supervisor = system.actorOf(Props(new Supervisor()))
        actorRefPromise.complete(Success(supervisor))
        _self.watch(supervisor)
      }

      override def postStop(): Unit = {
        buffer = Queue.empty[A]
      }

      setHandler(
        out,
        new OutHandler {
          override def onPull(): Unit = tryToProduce()

          override def onDownstreamFinish(): Unit = {
            log.debug("onDownstreamFinish: start")
            if (!isClosed(out)) {
              complete(out)
            }
            _self.become {
              case (_, ex: Exception) =>
                completionState = Some(Failure(ex))
                inFlight = 0
                checkForCompletion()
              case (_, Terminated(ref)) if ref == supervisor =>
                log.info(s"receive message = Terminated($ref); completeStage()")
                completionState = Some(Failure(new Exception("terminate actor")))
                inFlight = 0
                checkForCompletion()
            }
            _self.unwatch(supervisor)
            log.debug("onDownstreamFinish: finish")
          }
        }
      )

    }
    (logic, actorRefPromise.future)
  }

}
