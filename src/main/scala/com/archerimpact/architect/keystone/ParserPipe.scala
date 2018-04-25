package com.archerimpact.architect.keystone

import akka.actor.{ActorRef, Props}
import com.archerimpact.architect.keystone.shipments.{RawFile, Shipment}


object ParserPipe {
  def props(nextPipes: List[ActorRef]): Props = Props(new ParserPipe(nextPipes))
}

class ParserPipe(nextPipes: List[ActorRef]) extends PipeActor(nextPipes) {

  override def processShipment(shipment: Shipment): Shipment = shipment match {
    case rawFile: RawFile => rawFile.parser.rawFileToGraph(rawFile)
  }

  override def updateStats(): Unit = context.parent ! KeystoneSupervisor.IncParsed

}
