package com.archerimpact.architect.keystone

import akka.actor.{Actor, ActorLogging, ActorRef, ActorSystem, Props}
import com.archerimpact.architect.keystone.sinks.{ElasticSinkActor, Neo4jSinkActor, SinkActor}

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

  override def receive: Receive = {

    case StartPipeline =>
      val elasticSinkActor = context.actorOf(ElasticSinkActor.props(nextSink = None), "elastic-sink")
      val neo4jSinkActor = context.actorOf(Neo4jSinkActor.props(nextSink = Some(elasticSinkActor)), "neo4j-sink")
      val parserActor = context.actorOf(ParserActor.props(sink = neo4jSinkActor), "parser-supervisor")
      val loaderActor = context.actorOf(LoaderActor.props(parser = parserActor), "loader-actor")
      val rmqSourceActor = context.actorOf(RMQSourceActor.props(), "rmq-source-actor")
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

