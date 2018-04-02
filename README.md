# Akka2RaspberryPi
Control my Raspberry Pi 3B 's GPIO from Akka (with remote client)

# What's that about ?
This project is a test run of Akka with Raspberry Pi 3B.
It is made of two sbt sub-projects :
- akkaPiClient is a remote client (run it on your desktop)
- akkaPiController is a Pi GPIO controller implementing LEDs (wired on a breadboard)

# Network configuration notice
Both client & controller communicate through messages, based on TCP.
In this implementation, the following are assumed :
- the Raspberry Pi responds to 192.168.1.51:2553
- the remote client responds to 192.168.1.30:2552

This can be changed from the resources directories of both projects, and in PIClient.scala.

# GPIO breadboard configuration
The LEDs are wired to the GPIO using the following configuration from PIGpio.scala

```scala
  val leds = List(
    ("gLED", RaspiPin.GPIO_03),
    ("yLED", RaspiPin.GPIO_04),
    ("rLED", RaspiPin.GPIO_05))
```

which leads on the breadboard to
  #22 => Green LED
  #23 => Yellow LED
  #24 => Red LED
(do not forget the resistors)
