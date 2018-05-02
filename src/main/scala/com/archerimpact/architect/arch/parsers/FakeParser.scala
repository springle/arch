package com.archerimpact.architect.arch.parsers

import com.archerimpact.architect.arch.shipments.GraphShipment

class FakeParser extends Parser {
  override def parse(data: Array[Byte], url: String): GraphShipment = GraphShipment(List(), List(), url)
}
