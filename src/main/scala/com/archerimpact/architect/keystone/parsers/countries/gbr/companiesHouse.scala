package com.archerimpact.architect.keystone.parsers.countries.gbr

import com.archerimpact.architect.keystone.parsers.formats.JSONParser
import com.archerimpact.architect.keystone.shipments.GraphShipment
import org.json4s.JValue

class companiesHouse extends JSONParser {
  override def jsonToGraph(data: JValue, url: String): GraphShipment = ???
}
