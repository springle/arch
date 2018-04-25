package com.archerimpact.architect.keystone.pipes

import akka.actor.{ActorRef, Props}
import com.archerimpact.architect.keystone.KeystoneSupervisor
import com.archerimpact.architect.keystone.shipments.{GraphShipment, Shipment}

object MatcherPipe {
  def props(nextPipes: List[ActorRef]): Props = Props(new MatcherPipe(nextPipes))
}

class MatcherPipe(nextPipes: List[ActorRef]) extends PipeActor(nextPipes) {

  override def processShipment(shipment: Shipment): GraphShipment = shipment match {
    case graph: GraphShipment => log.error("TODO: implement matching"); graph
  }

  override def updateStats(): Unit = context.parent ! KeystoneSupervisor.IncMatched

}
