package com.archerimpact.architect.keystone.parsers.countries.usa

import com.archerimpact.architect.keystone.parsers.formats.JSONParser
import com.archerimpact.architect.keystone.shipments.{Entity, GraphShipment, Link}
import com.archerimpact.architect.ontology._
import org.json4s._
import org.json4s.JsonDSL._
import org.json4s.native.JsonMethods._

case class PartialGraph(entities: List[Entity] = List[Entity](), links: List[Link] = List[Link]())

class ofac extends JSONParser {

  /* Utility function to extract name */
  def getName(jv: JValue): String = compact(render(jv \ "identity" \ "primary" \ "display_name"))

  /* Utility function to extract identifying documents */
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

  /* Utility function to extract links */
  def getLinks(id: String, jv: JValue): List[Link] = jv match {
    case JArray(List()) => List[Link]()
    case JArray(links) =>
      links.children.map(link => Link(
        subjId = id,
        predicate = convertPredicate(compact(render(link \ "relation_type"))),
        objId = compact(render(link \ "linked_id"))
      ))
  }

  /* Utility function to transform predicate names */
  def convertPredicate(predicate: String): String = predicate match {
    case `predicate` if predicate.contains("Associate Of") => "ASSOCIATE_OF"
    case `predicate` if predicate.contains("Acting for or on behalf of") => "ACTING_FOR"
    case `predicate` if predicate.contains("Family member of") => "RELATED_TO"
    case `predicate` if predicate.contains("playing a significant role in") => "SIGNIFICANT_PART_OF"
    case `predicate` if predicate.contains("Providing support to") => "PROVIDING_SUPPORT_TO"
    case `predicate` if predicate.contains("Owned or Controlled By") => "OWNED_BY"
    case `predicate` if predicate.contains("Leader or official of") => "LEADER_OF"
  }

  def getPartialGraph(listing: JValue): PartialGraph = {

    /* Extract fields */
    val id = compact(render(listing \ "fixed_ref"))
    val name = getName(listing)
    val identifyingDocuments = getIdentifyingDocuments(listing \ "documents")
    val subtype = listing \ "party_sub_type"

    /* Determine entity type */
    val proto = subtype match {
      case JString("Entity") =>
        organization(name, identifyingDocuments)
      case JString("Individual") =>
        person(name, identifyingDocuments)
      case JString("Vessel") =>
        vessel(name, identifyingDocuments)
      case JString("Aircraft") =>
        aircraft(name, identifyingDocuments)
    }

    /* Generate partial graph */
    val `entities` = List(Entity(id, proto))
    val `links` = getLinks(id, listing \ "linked_profiles")
    PartialGraph(`entities`, `links`)
  }

  def merge(a: PartialGraph, b: PartialGraph): PartialGraph =
    a.copy(entities = a.entities ::: b.entities, links = a.links ::: b.links)

  override def jsonToGraph(data: JValue, url: String): GraphShipment = {

    /* Generate partial graph for each sanction listing */
    val partialGraphs = data.children.map(listing => getPartialGraph(listing))

    /* Merge into a single graph to return */
    val mergedGraph = partialGraphs.foldLeft(PartialGraph())(merge)
    GraphShipment(mergedGraph.entities, mergedGraph.links, url)
  }
}
