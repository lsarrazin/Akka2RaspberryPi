package org.lsa.akkapi.controller

import com.typesafe.config.ConfigFactory
import akka.actor.{ Actor, ActorLogging, ActorRef, ActorSystem, Props, actorRef2Scala }

import org.lsa.akkapi.common.PIMessages._
import org.lsa.akkapi.common.PIMessages.Color._

import org.lsa.akkapi.gpio.PILed
import org.lsa.akkapi.gpio.PIGpio
import org.lsa.akkapi.common.PIMessages

object PIController {
  
  var knownPins: Map[Color, ActorRef] = Map.empty
  
  def registerPin(color: Color, actor: ActorRef): Unit = 
    knownPins = knownPins + (color -> actor)
  
  def pinFor(color: Color): Option[ActorRef] = knownPins get color
    
  def main(args: Array[String]): Unit = {
    
    val system = ActorSystem("Sys", ConfigFactory.load("PI-controller"))
    val controller = system.actorOf(Props[PIController], "raspPi3B")
    
    val gpio: ActorRef = system.actorOf(PIGpio.props(), "piGpio")

    val rled: ActorRef = system.actorOf(PILed.props("rLED", gpio), "piRedLed")
    val yled: ActorRef = system.actorOf(PILed.props("yLED", gpio), "piYellowLed")
    val gled: ActorRef = system.actorOf(PILed.props("gLED", gpio), "piGreenLed")
    
    registerPin(Red, rled)
    registerPin(Yellow, yled)
    registerPin(Green, gled)
  }
  
}

class PIController extends Actor {
  
  import PIController.pinFor
  import PILed._
  

  def receive = {
    
    case PIMessages.LightOn(c: Color) => { 
      println("Received LightOn message for {} LED", c)
      val pin = pinFor(c)
      if (pin.isDefined) {
        println("- Activating LED " + pin)
        pin.get ! PILed.LightOn
        println("- Sending back status")
        sender ! Lighted(c) 
      }
    }
    
    case PIMessages.LightOff(c: Color) => {
      println("Received LightOff message for {} LED", c)
      val pin = pinFor(c)
      if (pin.isDefined) {
        println("- Dectivating LED " + pin)
        pin.get ! PILed.LightOff
        println("- Sending back status")
        sender ! Unlighted(c) 
      }
    }
    
    case PIMessages.Blink(c: Color, d: Long, p: Int) => {
      println("Received Blink message for {} LED", c)
      val pin = pinFor(c)
      if (pin.isDefined) {
        println("- Activating LED " + pin)
        pin.get ! PILed.Blink(d, p)
        println("- Sending back status")
        sender ! Blinked(c) 
      }
    }
      
    case Sleep(t: Long) =>
      println(s"- Sleeping for $t ms")
      Thread.sleep(t)
      sender ! Awake

    case Shutdown => context.system.terminate()
    case _        =>
  }
}

