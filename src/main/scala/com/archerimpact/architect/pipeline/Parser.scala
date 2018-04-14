package com.archerimpact.architect.pipeline

import akka.actor.{Actor, ActorLogging, Props}

object Parser {
  final case class ParseShipment(shipment: Shipment)
}

trait Parser extends Actor with ActorLogging

/* ------------------------------ */
/* A dummy DataParser for testing */
/* ------------------------------ */

object DummyParser {
  def props: Props = Props(new DummyParser)
}

class DummyParser extends Parser {
  import Parser._
  override def receive: PartialFunction[Any, Unit] = {
    case ParseShipment(shipment: Shipment) =>
      log.info(s"Parsing ${shipment.dataFormat} shipment from ${shipment.url}: ${shipment.data}")
  }
}
