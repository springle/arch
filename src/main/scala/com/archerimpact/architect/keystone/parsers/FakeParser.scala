package com.archerimpact.architect.keystone.parsers

import com.archerimpact.architect.keystone.shipments.GraphShipment

class FakeParser extends Parser {
  override def parse(data: Array[Byte], url: String): GraphShipment = GraphShipment(List(), List(), url)
}
