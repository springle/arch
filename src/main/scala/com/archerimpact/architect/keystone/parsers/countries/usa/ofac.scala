package com.archerimpact.architect.keystone.parsers.countries.usa

import com.archerimpact.architect.keystone.parsers.formats.XMLParser
import com.archerimpact.architect.keystone.shipments.{Entity, GraphShipment}

import scala.xml.Elem

class ofac extends XMLParser {

  private var entities = scala.collection.mutable.Set[Entity]()

  override def parseXML(root: Elem): GraphShipment = {
    val referenceValues = root \ "ReferenceValues"
    GraphShipment(List(), List(), "")
  }

}
