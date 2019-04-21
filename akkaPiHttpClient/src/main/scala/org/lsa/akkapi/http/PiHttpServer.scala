package org.lsa.akkapi.http

import scala.concurrent.Await
import scala.concurrent.duration.Duration

import akka.actor.{ ActorRef, ActorSystem }
import akka.http.scaladsl.Http
import akka.http.scaladsl.server.Route
import akka.stream.ActorMaterializer
import com.typesafe.config.ConfigFactory

object PiHttpServer extends App with LedRoutes {

  // set up ActorSystem and other dependencies here
  implicit val system: ActorSystem = ActorSystem("AkkaPiHttpServer", ConfigFactory.load("PI-client"))
  implicit val materializer: ActorMaterializer = ActorMaterializer()

  val remoteHostPort = "192.168.1.51:2553"
  val remotePath = s"akka.tcp://Sys@$remoteHostPort/user/raspPi3B"

  val ledRegistryActor: ActorRef = system.actorOf(LedRegistryActor.props(remotePath), "ledRegistryActor")

  // from the UserRoutes trait
  lazy val routes: Route = ledRoutes

  Http().bindAndHandle(routes, "localhost", 8080)

  println(s"Server online at http://localhost:8080/")

  Await.result(system.whenTerminated, Duration.Inf)
}
