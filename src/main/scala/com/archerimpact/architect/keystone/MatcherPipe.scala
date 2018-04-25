package com.archerimpact.architect.keystone

import akka.actor.{ActorRef, Props}
import com.archerimpact.architect.keystone.shipments.{Graph, Shipment}

object MatcherPipe {
  def props(nextPipes: List[ActorRef]): Props = Props(new MatcherPipe(nextPipes))
}

class MatcherPipe(nextPipes: List[ActorRef]) extends PipeActor(nextPipes) {

  override def processShipment(shipment: Shipment): Shipment = shipment match {
    case graph: Graph => log.error("TODO: implement matching"); graph
  }

  override def updateStats(): Unit = context.parent ! KeystoneSupervisor.IncMatched

}
