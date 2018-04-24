package com.archerimpact.architect.keystone

import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import com.archerimpact.architect.keystone.shipments.{Graph, RawDataShipment, Shipment}

import scala.concurrent.Future
import scala.util.{Failure, Success}
import scala.concurrent.ExecutionContext.Implicits.global

object ParserPipe {
  def props(connector: ActorRef): Props = Props(new ParserPipe(connector))
  final case class ParseShipment(shipment: Shipment)
}

class ParserPipe(val connector: ActorRef) extends Actor with ActorLogging {
  import ParserPipe._

  override def receive: Receive = {
    case ParseShipment(shipment: RawDataShipment) =>
      val f: Future[Graph] = shipment.parser.parseShipment(shipment)
      f.onComplete {
        case Success(graph: Graph) =>
          context.parent ! KeystoneSupervisor.IncParsed
          connector ! SinkActor.ForwardGraph(graph)
        case Failure(t) => log.error(s"Error when parsing ${shipment.url}: ${t.getMessage}")
      }
    case KeystoneSupervisor.IncParsed => context.parent ! KeystoneSupervisor.IncParsed
  }
}
