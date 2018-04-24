package com.archerimpact.architect.keystone

import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import com.archerimpact.architect.keystone.shipments.Shipment

import scala.concurrent.Future
import scala.util.{Failure, Success}
import scala.concurrent.ExecutionContext.Implicits.global

object LoaderPipe {
  def props(parser: ActorRef): Props = Props(new LoaderPipe(parser))
  final case class PackageShipment(url: String, dataFormat: String)
}

class LoaderPipe(val parser: ActorRef) extends Actor with ActorLogging {
  import LoaderPipe._

  override def receive: Receive = {
    case PackageShipment(url: String, dataFormat: String) =>
      val f: Future[Shipment] = Shipment.mkShipment(url, dataFormat)
      f onComplete {
        case Failure(t) => log.error(s"Error when packaging $url: ${t.getMessage}")
        case Success(shipment: Shipment) =>
          context.parent ! KeystoneSupervisor.IncLoaded
          parser ! ParserPipe.ParseShipment(shipment)
      }
  }
}
