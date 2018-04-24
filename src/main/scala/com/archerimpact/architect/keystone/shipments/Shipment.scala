package com.archerimpact.architect.keystone.shipments

import com.archerimpact.architect.keystone.parsers.Parser

object Shipment {
  def mkShipment(url: String, dataFormat: String): Shipment = url match {
    case `url` if url.startsWith("gs://") => new GoogleFile(url, dataFormat)
    case `url` if url.startsWith("dum://") => new FakeFile(url, dataFormat)
  }
}

trait Shipment

trait RawDataShipment extends Shipment {
  val url: String
  val dataFormat: String
  val data: Array[Byte]
  val country: String
  val sourceName: String
  val options: Map[String, String]
  val parser: Parser
}
