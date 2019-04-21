package org.lsa.akkapi.http

import akka.actor.{ActorRef, ActorSystem }
import akka.http.scaladsl.marshalling.Marshal
import akka.http.scaladsl.model._
import akka.http.scaladsl.testkit.ScalatestRouteTest

import com.typesafe.config.ConfigFactory

import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{ Matchers, WordSpec }

class LedRoutesSpec extends WordSpec with Matchers with ScalaFutures with ScalatestRouteTest
    with LedRoutes {
  
  val remoteHostPort = "192.168.1.51:2553"
  val remotePath = s"akka.tcp://Sys@$remoteHostPort/user/raspPi3B"
  
  val clientSystem = ActorSystem("Sys", ConfigFactory.load("PI-client"))

  // Here we need to implement all the abstract members of UserRoutes.
  // We use the real UserRegistryActor to test it while we hit the Routes, 
  // but we could "mock" it by implementing it in-place or by using a TestProbe() 
  override val ledRegistryActor: ActorRef =
    clientSystem.actorOf(LedRegistryActor.props(remotePath), "ledRegistry")

  lazy val routes = ledRoutes

  "LedRoutes" should {
    "return some LEDs (GET /leds)" in {
      // note that there's no need for the host part in the uri:
      val request = HttpRequest(uri = "/leds")

      request ~> routes ~> check {
        status should ===(StatusCodes.OK)

        // we expect the response to be json:
        contentType should ===(ContentTypes.`application/json`)

        // and no entries should be in the list:
        entityAs[String] should ===("""{"leds":[{"color":"Green"},{"color":"Yellow"},{"color":"Red"}]}""")
      }
    }

    "be able to switch on LED (POST /leds)" in {
      val led = Led("Green")
      val ledEntity = Marshal(led).to[MessageEntity].futureValue // futureValue is from ScalaFutures

      // using the RequestBuilding DSL:
      val request = Post("/leds/Green").withEntity(ledEntity)

      request ~> routes ~> check {
        status should ===(StatusCodes.OK)

        // we expect the response to be json:
        contentType should ===(ContentTypes.`application/json`)

        // and we know what message we're expecting back:
        entityAs[String] should ===("""{"description":"LED Green switched on."}""")
      }
    }

    "be able to switch off leds (DELETE /leds)" in {
      // user the RequestBuilding DSL provided by ScalatestRouteSpec:
      val request = Delete(uri = "/leds/Green")

      request ~> routes ~> check {
        status should ===(StatusCodes.OK)

        // we expect the response to be json:
        contentType should ===(ContentTypes.`application/json`)

        // and no entries should be in the list:
        entityAs[String] should ===("""{"description":"LED Green switched off."}""")
      }
    }
  }

}
