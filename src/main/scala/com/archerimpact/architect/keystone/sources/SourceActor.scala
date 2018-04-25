package com.archerimpact.architect.keystone.sources

import akka.actor.{Actor, ActorLogging, ActorRef}
import com.archerimpact.architect.keystone.{KeystoneSupervisor, PipeActor}
import com.archerimpact.architect.keystone.shipments.Shipment
import com.archerimpact.architect.keystone.sources.SourceActor.StartSending

object SourceActor {
  final case object StartSending
}

abstract class SourceActor(val target: ActorRef) extends Actor with ActorLogging {

  def startSending(): Unit

  def sendShipment(shipment: Shipment): Unit = {
    target ! PipeActor.ForwardShipment(shipment)
    context.parent ! KeystoneSupervisor.IncReceived
  }

  override def receive: Receive = {
    case StartSending => startSending()
  }

}
