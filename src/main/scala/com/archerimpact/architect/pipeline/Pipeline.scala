package com.archerimpact.architect.pipeline

import akka.actor.{ Actor, ActorLogging, ActorSystem, Props }

object Printer {
  def props: Props = Props[Printer]
  final case class OutputMessage(msg: String)
}

class Printer extends Actor with ActorLogging {
  import Printer._

  def receive = {
    case OutputMessage(msg) =>
      log.info(msg)
  }
}

object Pipeline extends App {
  import Printer._
  val system = ActorSystem("architectPipeline")
  val printerActor = system.actorOf(Printer.props)
  printerActor ! OutputMessage("Hello, printerActor.")
}
