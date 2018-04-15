package com.archerimpact.architect.keystone

import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import scala.concurrent.Future
import scala.util.{Success, Failure}
import scala.concurrent.ExecutionContext.Implicits.global

object Parser {
  final case class ParseShipment(shipment: Shipment)
}

trait Parser extends Actor with ActorLogging

/* ------------------------ */
/* A Supervisor for Parsers */
/* ------------------------ */

object ParserSupervisor {
  def props(connector: ActorRef): Props = Props(new ParserSupervisor(connector))
}

class ParserSupervisor(val connector: ActorRef) extends Actor with ActorLogging {
  import Parser._

  val myCSVParser: ActorRef = context.actorOf(CSVParser.props(connector), "csv-parser")
  val myDummyParser: ActorRef = context.actorOf(DummyParser.props(connector), "dummy-parser")

  def routeShipment(shipment: Shipment): Unit = shipment match {
    case `shipment` if shipment.dataFormat == "csv" =>
      myCSVParser ! ParseShipment(shipment)
    case `shipment` if shipment.dataFormat == "dummy" =>
      myDummyParser ! ParseShipment(shipment)
  }

  override def receive: Receive = {
    case ParseShipment(shipment: Shipment) => routeShipment(shipment)
  }
}

/* ---------------------- */
/* A Parser for CSV files */
/* ---------------------- */

object CSVParser {
  def props(connector: ActorRef): Props = Props(new CSVParser(connector))
}

class CSVParser(val connector: ActorRef) extends Parser {
  import Parser._

  def parseShipment(shipment: Shipment): Future[Graph] = {
    Future {
      Graph(List(), List(), shipment.url)
    }
  }

  override def receive: PartialFunction[Any, Unit] = {
    case ParseShipment(shipment: Shipment) =>
      log.info(s"Parsing ${shipment.dataFormat} shipment from ${shipment.url}")
      val f: Future[Graph] = parseShipment(shipment)

      f.onComplete {
        case Success(graph: Graph) => connector ! Connector.ForwardGraph(graph)
        case Failure(t) => log.error(s"Error when parsing ${shipment.url}: ${t.getMessage}")
      }

  }
}

/* -------------------------- */
/* A dummy Parser for testing */
/* -------------------------- */

object DummyParser {
  def props(connector: ActorRef): Props = Props(new DummyParser(connector))
}

class DummyParser(val connector: ActorRef) extends Parser {
  import Parser._
  override def receive: PartialFunction[Any, Unit] = {
    case ParseShipment(shipment: Shipment) =>
      log.info(s"Dummy parsing ${shipment.dataFormat} shipment from ${shipment.url}: ${shipment.data}")
  }
}
