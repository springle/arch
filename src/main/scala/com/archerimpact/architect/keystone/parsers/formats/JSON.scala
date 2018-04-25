package com.archerimpact.architect.keystone.parsers.formats

import com.archerimpact.architect.keystone.parsers.Parser
import com.archerimpact.architect.keystone.shipments.GraphShipment

class JSON extends Parser {

  override def parse(data: Array[Byte], url: String): GraphShipment = {
    GraphShipment(List(), List(), url)
  }

}
