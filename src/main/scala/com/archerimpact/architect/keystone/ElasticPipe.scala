package com.archerimpact.architect.keystone

import akka.actor.{ActorRef, Props}
import com.archerimpact.architect.keystone.shipments.{Graph, Shipment}

object ElasticPipe {
  def props(nextPipes: List[ActorRef]): Props = Props(new ElasticPipe(nextPipes))
}

class ElasticPipe(nextPipes: List[ActorRef]) extends PipeActor(nextPipes) {

  override def processShipment(shipment: Shipment): Shipment = shipment match {
    case graph: Graph => ???
  }

  override def updateStats(): Unit = context.parent ! KeystoneSupervisor.IncToElastic

}
