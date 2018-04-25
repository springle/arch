package com.archerimpact.architect.keystone.parsers

import com.archerimpact.architect.keystone.shipments.{GraphShipment, FileShipment}

trait Parser {
  def parse(data: Array[Byte], url: String): GraphShipment
  def fileToGraph(rawFile: FileShipment): GraphShipment = parse(rawFile.data, rawFile.url)
}
