package com.archerimpact.architect.keystone.pipes

import akka.actor.{ActorRef, Props}
import com.archerimpact.architect.keystone.KeystoneSupervisor
import com.archerimpact.architect.keystone.shipments.{FileShipment, GraphShipment, Shipment}

object ParserPipe {
  def props(nextPipes: List[ActorRef]): Props = Props(new ParserPipe(nextPipes))
}

class ParserPipe(nextPipes: List[ActorRef]) extends PipeActor(nextPipes) {

  override def processShipment(shipment: Shipment): GraphShipment = shipment match {
    case rawFile: FileShipment => rawFile.parser.fileToGraph(rawFile)
  }

  override def updateStats(): Unit = context.parent ! KeystoneSupervisor.IncParsed

}
