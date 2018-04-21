package com.archerimpact.architect.keystone

import akka.actor.{Actor, ActorLogging, ActorRef, ActorSystem, Props}

import scala.io.StdIn

object KeystoneSupervisor {
  def props: Props = Props(new KeystoneSupervisor)
  final case object StartPipeline
  final case object IncReceived
  final case object IncLoaded
  final case object IncParsed
  final case object IncDelivered
}

class KeystoneSupervisor extends Actor with ActorLogging {
  import KeystoneSupervisor._

  var loadedCount = 0
  var parsedCount = 0
  var deliveredCount = 0
  var receivedCount = 0

  override def preStart(): Unit = log.info("Keystone pipeline started")
  override def postStop(): Unit = log.info("Keystone pipeline stopped")

  override def receive: PartialFunction[Any, Unit] = {

    case StartPipeline =>
      val dummySinkActor: ActorRef = context.actorOf(SinkActor.props, "dummy-sink-actor")
      val parserActor: ActorRef = context.actorOf(ParserSupervisor.props(dummySinkActor), "parser-supervisor")
      val loaderActor: ActorRef = context.actorOf(LoaderActor.props(parserActor), "loader-actor")
      val rmqSourceActor: ActorRef = context.actorOf(RMQSourceActor.props(), "rmq-source-actor")
      rmqSourceActor ! SourceActor.StartSending(loaderActor)

    case IncReceived =>
      receivedCount += 1
      log.info(s"Received $receivedCount, Loaded $loadedCount, Parsed $parsedCount, Delivered $deliveredCount")

    case IncLoaded =>
      loadedCount += 1
      log.info(s"Received $receivedCount, Loaded $loadedCount, Parsed $parsedCount, Delivered $deliveredCount")

    case IncParsed =>
      parsedCount += 1
      log.info(s"Received $receivedCount, Loaded $loadedCount, Parsed $parsedCount, Delivered $deliveredCount")

    case IncDelivered =>
      deliveredCount += 1
      log.info(s"Received $receivedCount, Loaded $loadedCount, Parsed $parsedCount, Delivered $deliveredCount")

  }
}

object Keystone extends App {
  val system = ActorSystem("keystone-pipeline")
  try {
    val keystoneSupervisor = system.actorOf(KeystoneSupervisor.props, "keystone-supervisor")
    keystoneSupervisor ! KeystoneSupervisor.StartPipeline
    StdIn.readLine()
  } finally {
    system.terminate()
  }
}

