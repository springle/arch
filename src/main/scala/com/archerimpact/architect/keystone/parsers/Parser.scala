package com.archerimpact.architect.keystone.parsers

import com.archerimpact.architect.keystone.{Graph, Shipment}

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

trait Parser {

  def parse(data: Any, url: String): Graph

  def parseShipment(shipment: Shipment): Future[Graph] =
    Future { parse(shipment.data, shipment.url) }

}

/* -------------------------- */
/* A dummy parser for testing */
/* -------------------------- */

class DummyParser extends Parser {
  override def parse(data: Any, url: String): Graph = Graph(List(), List(), url)
}
