package com.archerimpact.architect.pipeline

import akka.actor.{ Actor, ActorLogging, Props }

object LoaderSupervisor {
  def props: Props = Props(new LoaderSupervisor)
  final case class StartLoading(dataSource: DataSource)
  final case class LoadShipment(shipment: Shipment)
}

class LoaderSupervisor extends Actor with ActorLogging {
  import LoaderSupervisor._

  override def preStart(): Unit = log.info("LoaderSupervisor started")
  override def postStop(): Unit = log.info("LoaderSupervisor stopped")

  override def receive: PartialFunction[Any, Unit] = {
    case StartLoading(dataSource: DataSource) =>
      dataSource.startSending(self)
      log.info(s"Started loading from $dataSource")

    case LoadShipment(shipment: Shipment) =>
      log.info(s"Loaded shipment: $shipment")
  }
}
