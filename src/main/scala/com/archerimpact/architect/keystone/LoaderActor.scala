package com.archerimpact.architect.keystone

import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import scala.concurrent.Future
import scala.util.{Success, Failure}
import scala.concurrent.ExecutionContext.Implicits.global

object LoaderActor {
  def props(parser: ActorRef): Props = Props(new LoaderActor(parser))
  final case class StartLoading(dataSource: ActorRef)
  final case class PackageShipment(url: String, dataFormat: String)
}

class LoaderActor(val parser: ActorRef) extends Actor with ActorLogging {
  import LoaderActor._

  override def receive: Receive = {

    case PackageShipment(url: String, dataFormat: String) =>
      log.info(s"Packaging $dataFormat shipment from $url")
      val f: Future[Shipment] = Shipment.packageShipment(url, dataFormat)

      f onComplete {
        case Success(shipment: Shipment) => parser ! ParserActor.ParseShipment(shipment)
        case Failure(t) => log.error(s"Error when packaging $url: ${t.getMessage}")
      }

  }
}
