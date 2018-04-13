package com.archerimpact.architect.pipeline

import akka.actor.{Actor, ActorLogging, ActorSystem, Props}
import scala.io.StdIn

object PipelineSupervisor {
  def props: Props = Props(new PipelineSupervisor)
  final case object StartPipeline
}

class PipelineSupervisor extends Actor with ActorLogging {
  import PipelineSupervisor._

  override def preStart(): Unit = log.info("PipelineSupervisor started")
  override def postStop(): Unit = log.info("PipelineSupervisor stopped")

  override def receive = {
    case StartPipeline =>
      val loaderSupervisor = context.actorOf(LoaderSupervisor.props, "loader-supervisor")
      val dummyDataSource = context.actorOf(DummyDataSource.props, "dummy-data-source")
      loaderSupervisor ! LoaderSupervisor.StartLoading(dummyDataSource)
  }
}

object Pipeline extends App {
  val system = ActorSystem("architect-pipeline")
  try {
    val pipelineSupervisor = system.actorOf(PipelineSupervisor.props, "pipeline-supervisor")
    pipelineSupervisor ! PipelineSupervisor.StartPipeline
    StdIn.readLine()
  } finally {
    system.terminate()
  }
}
