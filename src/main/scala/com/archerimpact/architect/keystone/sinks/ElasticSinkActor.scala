package com.archerimpact.architect.keystone.sinks

import akka.actor.{ActorRef, Props}
import com.archerimpact.architect.keystone.Graph

object ElasticSinkActor {
  def props(nextSink: Option[ActorRef]): Props = Props(new ElasticSinkActor(nextSink))
}

class ElasticSinkActor(nextSink: Option[ActorRef]) extends SinkActor(nextSink) {
  override def sinkGraph(graph: Graph): Unit = {
    log.info("Sending to elastic...")
  }
}
