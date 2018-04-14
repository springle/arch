package com.archerimpact.architect.pipeline

import akka.actor.{Actor, ActorLogging, ActorRef, Props}

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

  override def preStart(): Unit = log.info("DummyParser started")
  override def postStop(): Unit = log.info("DummyParser stopped")

  override def receive = {
    case ParseShipment(shipment: Shipment) =>
      log.info(s"Parsing shipment from ${shipment.dataSource}")
  }
}
