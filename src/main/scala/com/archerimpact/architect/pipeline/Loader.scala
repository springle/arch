package com.archerimpact.architect.pipeline

import akka.actor.{Actor, ActorLogging, ActorRef, Props}

object Loader {
  def props(parser: ActorRef): Props = Props(new Loader(parser))
  final case class StartLoading(dataSource: ActorRef)
  final case class LoadShipment(shipment: Shipment)
}

class Loader(
            val parser: ActorRef
            ) extends Actor with ActorLogging {
  import Loader._

  override def preStart(): Unit = log.info("Loader started")
  override def postStop(): Unit = log.info("Loader stopped")

  override def receive = {
    case StartLoading(dataSource: ActorRef) =>
      dataSource ! DataSource.StartSending(self)
      log.info(s"Started loading from $dataSource")

    case LoadShipment(shipment: Shipment) =>
      parser ! Parser.ParseShipment(shipment)
      log.info(s"Loading shipment from ${shipment.dataSource} for parsing")
  }
}
