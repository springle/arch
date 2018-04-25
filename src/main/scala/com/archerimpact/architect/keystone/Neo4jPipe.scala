package com.archerimpact.architect.keystone

import akka.actor.{ActorRef, Props}
import com.archerimpact.architect.keystone.shipments.{Graph, Shipment}

object Neo4jPipe {
  def props(nextPipes: List[ActorRef]): Props = Props(new Neo4jPipe(nextPipes))
}

class Neo4jPipe(nextPipes: List[ActorRef]) extends PipeActor(nextPipes) {
  override def processShipment(shipment: Shipment): Shipment = shipment match {
    case graph: Graph => graph
  }
  override def updateStats(): Unit = context.parent ! KeystoneSupervisor.IncToNeo4j
}
