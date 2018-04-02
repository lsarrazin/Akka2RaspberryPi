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

<img src="https://raw.githubusercontent.com/lsarrazin/Akka2RaspberryPi/master/Breadboard-small.jpg" alt="Breadboard with LEDs" height="277px" width="446px"/>

# Dependency
The GPIO is addressed through Pi4J (here http://pi4j.com/ or here https://github.com/Pi4J/pi4j/)
For convenience, it is fetched from sbt through

```scala
lazy val remoteMavenRepo = "Sonatype Snapshots" at "https://oss.sonatype.org/content/repositories/snapshots/"
resolvers += remoteMavenRepo

lazy val pi4jVersion = "1.1"
libraryDependencies += "com.pi4j" % "pi4j-core" % pi4jVersion
```

On my PI 3B that is not (yet) recognized, I need to push the following definition to the JVM `-Dpi4j.linking=dynamic`

This can easily be done through sbt using
```ksh
sbt -Dpi4j.linking=dynamic
```

