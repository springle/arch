package com.archerimpact.architect.keystone.shipments

class UrlShipment(
          val url: String
          ) extends Shipment {
  val fileFormat: String = url.split("\\.").last
}
