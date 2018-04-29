package com.archerimpact.architect.keystone.parsers.countries.usa

import com.archerimpact.architect.keystone.parsers.formats.JSONParser
import com.archerimpact.architect.keystone.shipments.{Entity, GraphShipment, Link}
import com.archerimpact.architect.ontology.{identifyingDocument, organization}
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

  def getLinks(id: BigInt, jv: JValue): List[Link] = jv match {
    case JArray(List()) => List[Link]()
    case JArray(links) =>
      links.children.map(link => Link(
        subjId = id.toString,
        predicate = compact(render(link \ "relation_type")),
        objId = compact(render(link \ "linked_id")).toString
      ))
  }

  def individualToGraph(id: BigInt, listing: JValue): PartialGraph = PartialGraph()
  def vesselToGraph(id: BigInt, listing: JValue): PartialGraph = PartialGraph()
  def aircraftToGraph(id: BigInt, listing: JValue): PartialGraph =  PartialGraph()

  def organizationToGraph(id: BigInt, listing: JValue): PartialGraph = {
    val entity = Entity(id.toString, organization(
      name = getName(listing),
      documents = getIdentifyingDocuments(listing \ "documents")
    ))
    PartialGraph(List(entity), getLinks(id, listing \ "linked_profiles"))
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
    val graph = GraphShipment(partialGraph.entities, partialGraph.links, url)
    println(graph)
    graph
  }
  def merge(a: PartialGraph, b: PartialGraph): PartialGraph =
    a.copy(entities = a.entities ::: b.entities, links = a.links ::: b.links)
}
