package org.lsa.akkapi.client

import scala.concurrent.duration._
import akka.actor.Actor
import akka.actor.ActorIdentity
import akka.actor.ActorRef
import akka.actor.Identify
import akka.actor.ReceiveTimeout
import akka.actor.Terminated
import akka.actor.Props
import akka.actor.ActorSystem
import com.typesafe.config.ConfigFactory
import akka.actor.ActorSelection.toScala
import akka.actor.actorRef2Scala

object PIClient {
  
  def main(args: Array[String]): Unit = {
    val system = ActorSystem("Sys", ConfigFactory.load("PI-client"))

    val remoteHostPort = if (args.length >= 1) args(0) else "192.168.1.51:2553"
    val remotePath = s"akka.tcp://Sys@$remoteHostPort/user/raspPi3B"
    
    system.actorOf(PIClient.props(remotePath), "snd")
  }

  def props(path: String): Props =
    Props(new PIClient(path))

  private case object Warmup
}

class PIClient(path: String) extends Actor {
  import org.lsa.akkapi.common.PIMessages._
  import org.lsa.akkapi.common.PIMessages.Color._
  import PIClient._
  
  // Traffic light simulator (with mad ending)
  val defaultProgram: Seq[Command] = List(
      LightOn(Green),
      Sleep(5000),
      LightOff(Green),
      LightOn(Yellow),
      Sleep(1000),
      LightOff(Yellow),
      LightOn(Red),
      Sleep(5000),
      LightOn(Yellow),
      Sleep(500),
      LightOn(Green),
      LightOff(Yellow),
      LightOff(Red),
      Sleep(5000),
      Blink(Red, 500, 10),
      Blink(Yellow, 250, 20),
      Blink(Green, 100, 50),
      Sleep(5000)
      )
      
  var program = defaultProgram

  context.setReceiveTimeout(3.seconds)
  sendIdentifyRequest()

  def sendIdentifyRequest(): Unit =
    context.actorSelection(path) ! Identify(path)

  def receive = identifying

  def identifying: Receive = {
    case ActorIdentity(`path`, Some(actor)) =>
      println(s"Remote actor found: $path")
      context.watch(actor)
      context.become(active(actor))
      context.setReceiveTimeout(Duration.Undefined)
      self ! Reset
    case ActorIdentity(`path`, None) => println(s"Remote actor not available: $path")
    case ReceiveTimeout              => sendIdentifyRequest()
  }

  def continue(actor: ActorRef) = {
    if (program.size > 0) {
      val nextCommand = program.head
      actor ! nextCommand
      program = program.tail
    } else {
      actor ! Shutdown
    }
  }
    
  def active(actor: ActorRef): Receive = {
    
    case Reset =>
      println("Reset client")
      program = defaultProgram
      continue(actor)
    
    case Lighted(c: Color) =>
      println(c + " is lighted")
      continue(actor)
    
    case Unlighted(c: Color) =>
      println(c + " is unlighted")
      continue(actor)
    
    case Blinked(c: Color) =>
      println(c + " has blinked")
      continue(actor)
      
    case Awake =>
      println("Now awake")
      continue(actor)
    
    case Terminated(`actor`) =>
      println("PI Controller terminated")
      context.system.terminate()

  }
}

