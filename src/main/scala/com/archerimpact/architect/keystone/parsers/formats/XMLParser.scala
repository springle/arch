package com.archerimpact.architect.keystone.parsers.formats

import java.io.ByteArrayInputStream

import com.archerimpact.architect.keystone.parsers.Parser
import com.archerimpact.architect.keystone.shipments.GraphShipment

import scala.xml.Elem

abstract class XMLParser extends Parser {

  def parseXML(root: Elem): GraphShipment

  override def parse(data: Array[Byte], url: String): GraphShipment = {
    val reader: ByteArrayInputStream = new ByteArrayInputStream(data)
    val root: Elem = scala.xml.XML.load(reader)
    parseXML(root)
  }

}
