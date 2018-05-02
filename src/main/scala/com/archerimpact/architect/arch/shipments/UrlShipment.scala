package com.archerimpact.architect.arch.shipments

class UrlShipment(
          val url: String
          ) extends Shipment {
  val fileFormat: String = url.split("\\.").last
}
