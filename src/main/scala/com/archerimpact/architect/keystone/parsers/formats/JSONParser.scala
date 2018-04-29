package com.archerimpact.architect.keystone.parsers.formats

import com.archerimpact.architect.keystone.parsers.Parser
import com.archerimpact.architect.keystone.shipments.GraphShipment
import org.json4s._
import org.json4s.native.JsonMethods.{parse=>parseJson}

abstract class JSONParser extends Parser {

  def jsonToGraph(data: JValue, url: String): GraphShipment

  override def parse(data: Array[Byte], url: String): GraphShipment =
    jsonToGraph(parseJson(data.map(_.toChar).mkString), url)

}
