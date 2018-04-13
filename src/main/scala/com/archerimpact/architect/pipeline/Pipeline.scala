package com.archerimpact.architect.pipeline

import akka.actor.{ Actor, ActorLogging, ActorSystem, Props }

object Printer {
  def props: Props = Props[Printer]
  final case class OutputMessage(msg: String)
}

class Printer extends Actor with ActorLogging {
  import Printer._

  def receive = {
    case "printit" =>
      val secondRef = context.actorOf(Props.empty, "second-actor")
      println(s"Second: $secondRef")
    case OutputMessage(msg) =>
      log.info(msg)
  }
}

class StartStopActor1 extends Actor {
  override def preStart(): Unit = {
    println("first started")
    context.actorOf(Props[StartStopActor2], "second")
  }
  override def postStop(): Unit = println("first stopped")
  override def receive: Receive = {
    case "stop" => context.stop(self)
  }
}

class StartStopActor2 extends Actor {
  override def preStart(): Unit = println("second started")
  override def postStop(): Unit = println("second stopped")
  override def receive: Receive = Actor.emptyBehavior
}

object Pipeline extends App {
  val system = ActorSystem("architectPipeline")
  val printerActor = system.actorOf(Printer.props, "first-actor")
  println(s"First: $printerActor")
  printerActor ! "printit"
}
