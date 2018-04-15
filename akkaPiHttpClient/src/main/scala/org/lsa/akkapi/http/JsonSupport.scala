package org.lsa.akkapi.http

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import spray.json._

trait JsonSupport extends SprayJsonSupport {

  // import the default encoders for primitive types (Int, String, Lists etc)
  import DefaultJsonProtocol._
  import LedRegistryActor._

  implicit val userJsonFormat = jsonFormat1(Led)
  implicit val usersJsonFormat = jsonFormat1(Leds)

  implicit val actionPerformedJsonFormat = jsonFormat1(ActionPerformed)
}
