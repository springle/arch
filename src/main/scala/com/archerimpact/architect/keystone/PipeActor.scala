package com.archerimpact.architect.keystone

import akka.actor.{Actor, ActorLogging, ActorRef}
import com.archerimpact.architect.keystone.shipments.Shipment

object PipeActor {
  final case class ForwardShipment(shipment: Shipment)
}

abstract class PipeActor(nextPipes: List[ActorRef]) extends Actor with ActorLogging {
  import PipeActor._

  override def preStart(): Unit = log.info("Pipe installed.")
  override def postStop(): Unit = log.info("Pipe removed.")

  def processShipment(shipment: Shipment): Shipment
  def updateStats(): Unit

  override def receive: Receive = {
    case ForwardShipment(shipment: Shipment) =>
      val processedShipment = processShipment(shipment)
      for (pipe <- nextPipes)
        pipe ! ForwardShipment(processedShipment)
      updateStats()
  }
}
