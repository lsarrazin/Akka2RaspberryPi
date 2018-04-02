package org.lsa.akkapi.common

object PIMessages {
  
  case object Shutdown
  case object Reset
  
  object Color extends Enumeration {
    type Color = Value
    val Red, Yellow, Green = Value
  }
  
  import Color._
  
  sealed trait Command
  case class LightOn(color: Color) extends Command
  case class LightOff(color: Color) extends Command
  case class Blink(color: Color, duration: Long, pulse: Int) extends Command
  case class Sleep(duration: Long) extends Command

  sealed trait Status
  case class Lighted(color: Color) extends Status
  case class Unlighted(color: Color) extends Status
  case class Blinked(color: Color) extends Status
  case object Awake extends Status
}