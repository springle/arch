package com.archerimpact.architect.keystone

import akka.actor.{Actor, ActorLogging, ActorRef, ActorSystem, Props}

import scala.io.StdIn

object PipelineSupervisor {
  def props: Props = Props(new PipelineSupervisor)
  final case object StartPipeline
}

class PipelineSupervisor extends Actor with ActorLogging {
  import PipelineSupervisor._

  override def preStart(): Unit = log.info("Keystone pipeline started")
  override def postStop(): Unit = log.info("Keystone pipeline stopped")

  override def receive: PartialFunction[Any, Unit] = {
    case StartPipeline =>
      val connector: ActorRef =
        context.actorOf(Connector.props, "connector")
      val parserSupervisor: ActorRef =
        context.actorOf(ParserSupervisor.props(connector), "parser-supervisor")
      val loader: ActorRef =
        context.actorOf(Loader.props(parserSupervisor), "loader")
      val rmqDataSource: ActorRef =
        context.actorOf(RMQDataSource.props(), "rmq-data-source")
      loader ! Loader.StartLoading(rmqDataSource)
  }
}

object Keystone extends App {
  val system = ActorSystem("keystone-pipeline")
  try {
    val pipelineSupervisor = system.actorOf(PipelineSupervisor.props, "pipeline-supervisor")
    pipelineSupervisor ! PipelineSupervisor.StartPipeline
    StdIn.readLine()
  } finally {
    system.terminate()
  }
}

