package org.lsa.akkapi.gpio

import akka.actor.{ Actor, ActorRef, Props}
import akka.actor.actorRef2Scala

import scala.concurrent.duration._

object PILed {
  
  // Props
  def props(color: String, controller: ActorRef): Props = Props(new PILed(color, controller))

  // Messages
  case class GetStatus()
  case object LightOn
  case object LightOff
  case class  Blink(duration: Long, pulse: Int)

  case class  BlinkContinuation(duration: Long, pulse: Int)
  case class  BlinkTermination(duration: Long, pulse: Int)
}

class PILed(color: String, controller: ActorRef) extends Actor
  with akka.actor.ActorLogging {

  import PILed._
  import PIGpio._

  override def receive: Receive = {

    case b @ BlinkContinuation(duration: Long, pulse: Int) =>
      controller ! Low(color)
      context.system.scheduler.scheduleOnce(duration millis, self, BlinkTermination(duration, pulse))(context.system.dispatcher)

    case b @ BlinkTermination(duration: Long, pulse: Int) =>
      self ! Blink(duration, pulse - 1)

    case b @ Blink(duration: Long, pulse: Int) =>
      controller ! High(color)
      context.system.scheduler.scheduleOnce(duration millis, self, BlinkContinuation(duration, pulse))(context.system.dispatcher)

    case LightOn =>
      controller ! High(color)

    case LightOff =>
      controller ! Low(color)

    case GetStatus() =>
      controller forward GetState(color)
  }
}


