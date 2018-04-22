package com.archerimpact.architect.keystone

import akka.actor.{Actor, ActorLogging, Props}

object SinkActor {
  def props: Props = Props(new SinkActor)
  final case class ForwardGraph(graph: Graph)
}

class SinkActor extends Actor with ActorLogging
{
  import SinkActor._

  override def receive: PartialFunction[Any, Unit] = {
    case ForwardGraph(graph: Graph) =>
      context.parent ! KeystoneSupervisor.IncDelivered
      for (entity <- graph.entities)
        log.info(entity.toString)
  }
}
