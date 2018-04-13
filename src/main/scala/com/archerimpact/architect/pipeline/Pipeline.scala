package com.archerimpact.architect.pipeline

import akka.actor.{Actor, ActorLogging, ActorSystem, Props}
import scala.io.StdIn

object PipelineSupervisor {
  def props(): Props = Props(new PipelineSupervisor)
}

class PipelineSupervisor extends Actor with ActorLogging {
  override def preStart(): Unit = log.info("Architect Pipeline started")
  override def postStop(): Unit = log.info("Architect Pipeline stopped")
  override def receive = Actor.emptyBehavior
}

object Pipeline extends App {
  val system = ActorSystem("architectPipeline")
  try {
    system.actorOf(PipelineSupervisor.props(), "pipeline-supervisor")
    StdIn.readLine()
  } finally {
    system.terminate()
  }
}
