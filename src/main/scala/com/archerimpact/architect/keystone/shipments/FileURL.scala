package com.archerimpact.architect.keystone.shipments

class FileURL(
          val url: String
          ) extends Shipment {
  val fileFormat: String = url.split("\\.").last
}
