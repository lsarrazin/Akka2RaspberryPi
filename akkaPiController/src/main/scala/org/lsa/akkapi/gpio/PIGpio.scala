package org.lsa.akkapi.gpio

import com.typesafe.config.ConfigFactory

import akka.actor.{ Actor, ActorSystem, ActorLogging, ActorRef, Props }
import akka.event.Logging

import com.pi4j.io.gpio.{ GpioFactory, GpioController, GpioPinDigitalOutput, Pin, PinState, RaspiPin }
import java.io.File

// Led companion / messages
object PIGpio {

  val leds = List(
    ("gLED", RaspiPin.GPIO_03),
    ("yLED", RaspiPin.GPIO_04),
    ("rLED", RaspiPin.GPIO_05),
    ("bLED", RaspiPin.GPIO_06))
  
  // Props
  def props(): Props = Props(new PIGpio(leds))

  // Messages
  final case class High(name: String)
  final case class Low(name: String)
  final case class GetState(name: String)
  final case class StatusResponse(name: String, status: String)
  final case object Acknowledge

}

// Pin actor
class PIGpio(pins: Seq[(String, Pin)]) extends Actor
  with akka.actor.ActorLogging {

  import PIGpio._

  // val log = Logging(context.system, this)
  var gpio: Option[GpioController] = None
  var leds: Map[String, GpioPinDigitalOutput] = Map()
  var cled: Option[GpioPinDigitalOutput] = None

  def initController: Unit = {
    val controller = GpioFactory.getInstance

    leds = pins.map {
        case (name, pin) => 
          val led = controller.provisionDigitalOutputPin(pin, name, PinState.LOW)
          led.setShutdownOptions(true, PinState.LOW)
          (name, led)
      }.toMap
    cled = leds.get("bLED")
    gpio = Some(controller)
  }

  override def preStart(): Unit = {
    super.preStart

    log.info(s"actor starting...")
    initController
  }

  override def preRestart(reason: Throwable, message: Option[Any]): Unit = {
    super.preRestart(reason,message) // stops all children, calls postStop( ) for crashing actor
    log.info(s"actor restarting...")
    initController
  }

  override def postRestart(reason: Throwable): Unit = {
    log.info(s"actor restarted...")
  }

  override def postStop(): Unit = {
    log.info(s"actor stopping...")
    gpio.map(_.shutdown)
  }

  def pinState(pin: GpioPinDigitalOutput): String = {
    (pin.isHigh, pin.isLow) match {
      case (true, false) => "On"
      case (false, true) => "Off"
      case _ => "Abnormal"
    }
  }

  def getPin(name: String): Option[(GpioPinDigitalOutput, String, String)] = {
    leds.get(name).map { 
      led =>
        val state = pinState(led)
        (led, name, state)
    }
  }
  
  def receive = {
    case Acknowledge => {
        cled.get.high
        Thread.sleep(50)
        cled.get.low
      }
    
    case High(name) => {
        self ! Acknowledge
        val led = getPin(name)
        if (led.isDefined) {
          log.info("Turning LED {} on", name)
          led.get._1.high
        } else {
          log.error("LED {} is not defined", name )
        }
      }
    
    case Low(name) => {
        self ! Acknowledge
        val led = getPin(name)
        if (led.isDefined) {
          log.info("Turning LED {} off", name)
          led.get._1.low
        } else {
          log.error("LED {} is not defined", name )
        }
      }
    
    case GetState(name) => {
        self ! Acknowledge
        val status = getPin(name).getOrElse((null, name, "undefined"))
        log.error("LED {} is {}", status._2, status._3)
        sender() ! StatusResponse(status._2, status._3)
      }
    
  }
}

