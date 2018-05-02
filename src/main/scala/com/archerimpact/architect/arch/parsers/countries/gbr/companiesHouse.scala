package com.archerimpact.architect.arch.parsers.countries.gbr

import com.archerimpact.architect.arch.parsers.formats.JSONParser
import com.archerimpact.architect.arch.shipments.GraphShipment
import org.json4s.JValue

class companiesHouse extends JSONParser {
  override def jsonToGraph(data: JValue, url: String): GraphShipment = ???
}
