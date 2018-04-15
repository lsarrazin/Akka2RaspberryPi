package org.lsa.akkapi.http

import akka.actor.{ ActorRef, ActorSystem }
import akka.event.Logging
import scala.concurrent.duration._

import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.server.directives.MethodDirectives.delete
import akka.http.scaladsl.server.directives.MethodDirectives.get
import akka.http.scaladsl.server.directives.MethodDirectives.post
import akka.http.scaladsl.server.directives.RouteDirectives.complete
import akka.http.scaladsl.server.directives.PathDirectives.path

import scala.concurrent.Future
import akka.pattern.ask
import akka.util.Timeout

import org.lsa.akkapi.http.LedRegistryActor._

trait LedRoutes extends JsonSupport {

  // we leave these abstract, since they will be provided by the App
  implicit def system: ActorSystem

  lazy val log = Logging(system, classOf[LedRoutes])

  // other dependencies that UserRoutes use
  def ledRegistryActor: ActorRef

  // Required by the `ask` (?) method below
  implicit lazy val timeout = Timeout(5.seconds) // usually we'd obtain the timeout from the system's configuration

  lazy val ledRoutes: Route =
    pathPrefix("leds") {
      concat(

        pathEnd {
          concat(
            get {
              val leds: Future[Leds] =
                (ledRegistryActor ? GetLeds).mapTo[Leds]
              complete(leds)
            }
          )
        },

        path(Segment) { color =>
          concat(
            get {
              val maybeLed: Future[Option[Led]] =
                (ledRegistryActor ? GetLed(color)).mapTo[Option[Led]]
              rejectEmptyResponse {
                complete(maybeLed)
              }
            },
            post {
              val ledLighted: Future[ActionPerformed] =
                (ledRegistryActor ? SwitchOnLed(color)).mapTo[ActionPerformed]
              onSuccess(ledLighted) { performed =>
                log.info("LED [{}] lighting posted: {}", color, performed.description)
                complete((StatusCodes.OK, performed))
              }
            },
            delete {
              val ledUnlighted: Future[ActionPerformed] =
                (ledRegistryActor ? SwitchOffLed(color)).mapTo[ActionPerformed]
              onSuccess(ledUnlighted) { performed =>
                log.info("LED [{}] unlighting posted: {}", color, performed.description)
                complete((StatusCodes.OK, performed))
              }
            }
          )
        }
      )
    }
}
