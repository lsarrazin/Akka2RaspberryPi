package org.lsa.akkapi.http

import scala.concurrent.duration._

import akka.actor.{ Actor, ActorIdentity, ActorRef, ActorSystem, ActorLogging, Identify, Props }
import akka.actor.ReceiveTimeout
import akka.actor.Terminated

import com.typesafe.config.ConfigFactory

import akka.actor.ActorSelection.toScala
import akka.actor.actorRef2Scala

import akka.event.Logging

final case class Led(color: String)
final case class Leds(leds: Seq[Led])

object LedRegistryActor {
  import org.lsa.akkapi.common.PIMessages._
  import org.lsa.akkapi.common.PIMessages.Color._

  final case class ActionPerformed(description: String)
  final case object GetLeds
  final case class SwitchOnLed(color: String)
  final case class SwitchOffLed(color: String)
  final case class BlinkLed(color: String)
  final case class GetLed(color: String)

  def props(path: String): Props = Props(new LedRegistryActor(path))
}

class LedRegistryActor(path: String) extends Actor with ActorLogging {
  import org.lsa.akkapi.common.PIMessages._
  import org.lsa.akkapi.common.PIMessages.Color._
  import LedRegistryActor._

  override lazy val log = Logging(context.system, classOf[LedRegistryActor])

  var leds = Set(
    Led("Green"),
    Led("Yellow"),
    Led("Red")
  )

  override def preStart(): Unit = {
    super.preStart

    log.info(s"PILedClient actor starting...")
  }

  override def preRestart(reason: Throwable, message: Option[Any]): Unit = {
    super.preRestart(reason, message) // stops all children, calls postStop( ) for crashing actor
    log.info(s"PILedClient actor restarting...")
  }

  override def postRestart(reason: Throwable): Unit = {
    log.info(s"PILedClient actor restarted...")
  }

  override def postStop(): Unit = {
    log.info(s"PILedClient actor stopping...")
  }

  context.setReceiveTimeout(3.seconds)
  sendIdentifyRequest()

  def sendIdentifyRequest(): Unit =
    context.actorSelection(path) ! Identify(path)

  def receive = identifying

  def identifying: Receive = {
    case ActorIdentity(`path`, Some(actor)) =>
      log.info(s"Remote actor found: $path")
      context.watch(actor)
      context.become(active(actor))
      context.setReceiveTimeout(Duration.Undefined)
      self ! Reset
    case ActorIdentity(`path`, None) => log.warning(s"Remote actor not available: $path")
    case ReceiveTimeout => sendIdentifyRequest()
  }

  def colorFrom(col: String): Color = col match {
    case "Green" => Green
    case "green" => Green
    case "Yellow" => Yellow
    case "yellow" => Yellow
    case "Red" => Red
    case "red" => Red
  }

  def active(controler: ActorRef): Receive = {
    case GetLeds =>
      sender() ! Leds(leds.toSeq)

    case SwitchOnLed(color: String) =>
      controler ! LightOn(colorFrom(color))
      // Light on led
      sender ! ActionPerformed(s"LED ${color} switched on.")

    case SwitchOffLed(color: String) =>
      controler ! LightOff(colorFrom(color))
      // Light off led
      sender ! ActionPerformed(s"LED ${color} switched off.")

    case BlinkLed(color: String) =>
      controler ! Blink(colorFrom(color), 250, 8)
      // Blink led
      sender ! ActionPerformed(s"LED ${color} blinked.")

    case GetLed(color: String) =>
      // Get led status
      sender ! ActionPerformed(s"LED ${color} is ???.")

    case Lighted(c: Color) =>
      log.info(c + " is lighted")
    
    case Unlighted(c: Color) =>
      log.info(c + " is unlighted")
    
    case Blinked(c: Color) =>
      log.info(c + " has blinked")
      
    case Terminated(`controler`) =>
      log.info("Led Registry terminated")
      controler ! Shutdown
      context.system.terminate()

  }

}
