package com.archerimpact.architect.keystone.parsers.countries.global

import java.nio.charset.StandardCharsets

import net.liftweb.json.DefaultFormats
import net.liftweb.json._
import com.archerimpact.architect.keystone.parsers.formats.JSON
import com.archerimpact.architect.keystone.shipments.GraphShipment

class openCorporates extends JSON {
  override def parse(data: Array[Byte], url: String): GraphShipment = {

    var dataStr = new String(data, StandardCharsets.UTF_8)
    print(dataStr)

    var parsedJSON = net.liftweb.json.parse(dataStr)
    var children = (parsedJSON \\ "company").children
    for (i <- children) {
      print(i)
    }


    GraphShipment(List(),List(), url)
  }
}
