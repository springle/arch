package com.archerimpact.architect.keystone

import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import scala.concurrent.Future
import scala.util.{Success, Failure}
import scala.concurrent.ExecutionContext.Implicits.global

object Loader {
  def props(parser: ActorRef): Props = Props(new Loader(parser))
  final case class StartLoading(dataSource: ActorRef)
  final case class PackageShipment(url: String, dataFormat: String)
}

class Loader(
              val parser: ActorRef
            ) extends Actor with ActorLogging {
  import Loader._

  override def receive: PartialFunction[Any, Unit] = {
    case StartLoading(dataSource: ActorRef) =>
      dataSource ! DataSource.StartSending(self)
      log.info(s"Started loading from $dataSource")

    case PackageShipment(url: String, dataFormat: String) =>
      log.info(s"Packaging shipment from $url")
      val f: Future[Shipment] = Shipment.packageShipment(url, dataFormat)
      f onComplete {
        case Success(shipment: Shipment) => parser ! Parser.ParseShipment(shipment)
        case Failure(t) => log.error(s"Error when packaging $url: ${t.getMessage}")
      }
  }
}
