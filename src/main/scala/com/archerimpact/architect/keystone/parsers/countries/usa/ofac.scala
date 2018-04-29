package com.archerimpact.architect.keystone.parsers.countries.usa

import com.archerimpact.architect.keystone.parsers.formats.JSONParser
import com.archerimpact.architect.keystone.shipments.{Entity, GraphShipment, Link}
import com.archerimpact.architect.ontology.identifyingDocument
import org.json4s._
import org.json4s.JsonDSL._
import org.json4s.native.JsonMethods._

case class PartialGraph(entities: List[Entity] = List[Entity](), links: List[Link] = List[Link]())

class ofac extends JSONParser {

  def getName(jv: JValue): String = compact(render(jv \ "identity" \ "primary" \ "display_name"))
  def getIdentifyingDocuments(jv: JValue): List[identifyingDocument] = jv match {
    case JArray(List()) => List[identifyingDocument]()
    case JArray(documents) =>
      documents.children.map(document => identifyingDocument(
        number = compact(render(document \ "id_number")),
        numberType = compact(render(document \ "type")),
        issuedBy = compact(render(document \ "issued_by")),
        issuedIn = compact(render(document \ "issued_in")),
        valid = document \ "validity" == JString("Valid")
      ))
  }
  def getLinks(jv: JValue): List[Link] = {

  }

  def individualToGraph(id: BigInt, listing: JValue): PartialGraph = PartialGraph()
  def vesselToGraph(id: BigInt, listing: JValue): PartialGraph = PartialGraph()
  def aircraftToGraph(id: BigInt, listing: JValue): PartialGraph =  PartialGraph()
  def organizationToGraph(id: BigInt, listing: JValue): PartialGraph = {
    val name: String = getName(listing)
    val documents: Seq[identifyingDocument] = getIdentifyingDocuments(listing \ "documents")
    val links: List[Link]() = getLinks(listing \ "linked_profiles")


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
