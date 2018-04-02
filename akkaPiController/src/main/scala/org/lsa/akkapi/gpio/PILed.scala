package org.lsa.akkapi.gpio

import akka.actor.{ Actor, ActorRef, Props}
import akka.actor.actorRef2Scala

object PILed {
  
  import org.lsa.akkapi.common.PIMessages._
  
  // Props
  def props(color: String, controller: ActorRef): Props = Props(new PILed(color, controller))

  // Messages
  case class GetStatus()
  case object LightOn
  case object LightOff
  case class  Blink(duration: Long, pulse: Int)
}

import org.lsa.akkapi.common.PIMessages.Color.Color

class PILed(color: String, controller: ActorRef) extends Actor
  with akka.actor.ActorLogging {

  import PILed._
  import PIGpio._

  override def receive: Receive = {
    
  case b @ Blink(duration: Long, pulse: Int) => {
      controller ! High(color)
      Thread.sleep(duration)
      controller ! Low(color)
      Thread.sleep(duration)
      self ! b.copy(duration, pulse-1)
    }
    
    case LightOn => {
      controller ! High(color)
    }
    
    case LightOff => {
      controller ! Low(color)
    }
    
    case GetStatus() => {
      controller forward GetState(color)
    }
    
  }
}


