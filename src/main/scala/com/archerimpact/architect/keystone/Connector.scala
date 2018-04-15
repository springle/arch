package com.archerimpact.architect.keystone

import akka.actor.{Actor, ActorLogging, Props}

object Connector {
  def props: Props = Props(new Connector)
  final case class ForwardGraph(graph: Graph)
}

class Connector extends Actor with ActorLogging
{
  import Connector._

  override def receive: PartialFunction[Any, Unit] = {
    case ForwardGraph(graph: Graph) =>
      log.info(s"Forwarding graph from ${graph.url} to connectors")
  }
}
