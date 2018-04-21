package com.archerimpact.architect.keystone

import akka.actor.{Actor, ActorLogging, ActorRef, Props}

import scala.concurrent.Future
import scala.util.{Failure, Success}
import scala.concurrent.ExecutionContext.Implicits.global

/* --------------------------------- */
/* An abstract Parser implementation */
/* --------------------------------- */

object ParserActor {
  final case class ParseShipment(shipment: Shipment)
}

abstract class ParserActor(connector: ActorRef) extends Actor with ActorLogging {
  import ParserActor._

  def parse(data: Any, url: String): Graph

  def parseShipment(shipment: Shipment): Future[Graph] =
    Future { parse(shipment.data, shipment.url) }

  override def receive: PartialFunction[Any, Unit] = {
    case ParseShipment(shipment: Shipment) =>
      log.info(s"Parsing ${shipment.dataFormat} shipment from ${shipment.url}")
      val f: Future[Graph] = parseShipment(shipment)
      f.onComplete {
        case Success(graph: Graph) => connector ! SinkActor.ForwardGraph(graph)
        case Failure(t) => log.error(s"Error when parsing ${shipment.url}: ${t.getMessage}")
      }
  }
}

/* ------------------------ */
/* A Supervisor for Parsers */
/* ------------------------ */

object ParserSupervisor {
  def props(connector: ActorRef): Props = Props(new ParserSupervisor(connector))
}

class ParserSupervisor(val connector: ActorRef) extends Actor with ActorLogging {
  import ParserActor._
  import parsers._

  val myUsaRouter: ActorRef = context.actorOf(usa.Router.props(connector), "usa-router")
  val myDummyParser: ActorRef = context.actorOf(DummyParserActor.props(connector), "dummy-parser")

  def routeShipment(shipment: Shipment): Unit = shipment match {
    case `shipment` if shipment.country == "dummy" =>
      myDummyParser ! ParseShipment(shipment)
    case `shipment` if shipment.country == "usa" =>
      myUsaRouter ! ParseShipment(shipment)
  }

  override def receive: Receive = {
    case ParseShipment(shipment: Shipment) => routeShipment(shipment)
  }
}

/* -------------------------- */
/* A dummy Parser for testing */
/* -------------------------- */

object DummyParserActor {
  def props(connector: ActorRef): Props = Props(new DummyParserActor(connector))
}

class DummyParserActor(val connector: ActorRef) extends ParserActor(connector) {
  override def parse(data: Any, url: String): Graph =
    Graph(List(), List(), url)
}
