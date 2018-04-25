package com.archerimpact.architect.keystone.parsers

import com.archerimpact.architect.keystone.shipments.Graph

class FakeParser extends Parser {
  override def parse(data: Array[Byte], url: String): Graph = Graph(List(), List(), url)
}
