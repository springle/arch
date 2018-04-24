package com.archerimpact.architect.keystone.sinks

import akka.actor.{Actor, ActorLogging, ActorRef}
import com.archerimpact.architect.keystone.{Graph, KeystoneSupervisor}

object SinkActor {
  final case class ForwardGraph(graph: Graph)
}

abstract class SinkActor(val nextSink: Option[ActorRef]) extends Actor with ActorLogging
{
  import SinkActor._

  def sinkGraph(graph: Graph): Unit

  override def receive: PartialFunction[Any, Unit] = {
    case ForwardGraph(graph: Graph) =>
      sinkGraph(graph)
      nextSink match {
        case Some(nextSink: ActorRef) => nextSink ! ForwardGraph(graph)
        case _ => context.parent ! KeystoneSupervisor.IncDelivered
    }
  }
}
