package com.archerimpact.architect.keystone.parsers.countries.usa

import com.archerimpact.architect.keystone.parsers.formats.JSONParser
import com.archerimpact.architect.keystone.shipments.{Entity, GraphShipment, Link}
import com.archerimpact.architect.ontology.identifyingDocument
import org.json4s._
import org.json4s.JsonDSL._
import org.json4s.native.JsonMethods._

case class PartialGraph(entities: List[Entity] = List[Entity](), links: List[Link] = List[Link]())

class ofac extends JSONParser {

  def getDocumentEntities(documents: JValue): List[identifyingDocument] = documents match {
    case JNull => List[identifyingDocument]()
    case _ => ???
  }

  def individualToGraph(id: BigInt, listing: JValue): PartialGraph = PartialGraph()
  def vesselToGraph(id: BigInt, listing: JValue): PartialGraph = PartialGraph()
  def aircraftToGraph(id: BigInt, listing: JValue): PartialGraph =  PartialGraph()
  def organizationToGraph(id: BigInt, listing: JValue): PartialGraph = {
    var links = List[Link]()
    val name: String = compact(render(listing \ "identity" \ "primary" \ "display_name"))
    val documents: List[identifyingDocument] = getDocumentEntities(listing \ "documents")




    PartialGraph()
  }


  override def jsonToGraph(data: JValue, url: String): GraphShipment = {
    val partialGraph = (for {
      listing: JValue <- data.children
      JInt(id) = listing \ "fixed_ref"
      JString(subtype) = listing \ "party_sub_type"
    } yield subtype match {
      case "Entity" => organizationToGraph(id, listing)
      case "Individual" => individualToGraph(id, listing)
      case "Vessel" => vesselToGraph(id, listing)
      case "Aircraft" => aircraftToGraph(id, listing)
    }).foldLeft(PartialGraph())(merge)
    GraphShipment(partialGraph.entities, partialGraph.links, url)
  }
  def merge(a: PartialGraph, b: PartialGraph): PartialGraph =
    a.copy(entities = a.entities ::: b.entities, links = a.links ::: b.links)
}
