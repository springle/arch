package com.archerimpact.architect.keystone

import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import com.archerimpact.architect.keystone.sinks.SinkActor

import scala.concurrent.Future
import scala.util.{Failure, Success}
import scala.concurrent.ExecutionContext.Implicits.global

object ParserActor {
  def props(sink: ActorRef): Props = Props(new ParserActor(sink))
  final case class ParseShipment(shipment: Shipment)
}

class ParserActor(val sink: ActorRef) extends Actor with ActorLogging {
  import ParserActor._

  override def receive: Receive = {
    case ParseShipment(shipment: Shipment) =>
      val f: Future[Graph] = shipment.parser.parseShipment(shipment)
      f.onComplete {
        case Success(graph: Graph) =>
          context.parent ! KeystoneSupervisor.IncParsed
          sink ! SinkActor.ForwardGraph(graph)
        case Failure(t) => log.error(s"Error when parsing ${shipment.url}: ${t.getMessage}")
      }
    case KeystoneSupervisor.IncParsed => context.parent ! KeystoneSupervisor.IncParsed
  }
}
