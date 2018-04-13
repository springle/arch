package com.archerimpact.architect.pipeline

import akka.actor.{Actor, ActorLogging, ActorRef, Props}

object LoaderSupervisor {
  def props: Props = Props(new LoaderSupervisor)
  final case class StartLoading(dataSource: ActorRef)
  final case class LoadShipment(shipment: Shipment)
}

class LoaderSupervisor extends Actor with ActorLogging {
  import LoaderSupervisor._

  override def preStart(): Unit = log.info("LoaderSupervisor started")
  override def postStop(): Unit = log.info("LoaderSupervisor stopped")

  override def receive = {
    case StartLoading(dataSource: ActorRef) =>
      dataSource ! DataSource.StartSending(self)
      log.info(s"Started loading from $dataSource")

    case LoadShipment(shipment: Shipment) =>
      log.info(s"Sending shipment from ${shipment.dataSource} to processing")
  }
}
