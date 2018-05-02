package com.archerimpact.architect.arch.parsers

import com.archerimpact.architect.arch.shipments.{GraphShipment, FileShipment}

trait Parser {
  def parse(data: Array[Byte], url: String): GraphShipment
  def fileToGraph(rawFile: FileShipment): GraphShipment = parse(rawFile.data, rawFile.url)
}
