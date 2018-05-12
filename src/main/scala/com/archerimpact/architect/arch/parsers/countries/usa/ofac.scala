package com.archerimpact.architect.arch.parsers.countries.usa

import com.archerimpact.architect.arch.parsers.formats.JSONParser
import com.archerimpact.architect.arch.shipments.{Entity, GraphShipment, Link}
import com.archerimpact.architect.ontology._
import org.json4s._
import org.json4s.JsonDSL._
import org.json4s.native.JsonMethods._

case class PartialGraph(entities: List[Entity] = List[Entity](), links: List[Link] = List[Link]())

class ofac extends JSONParser {

  /* Utility function to extract name */
  def getName(jv: JValue): String = compact(render(jv \ "identity" \ "primary" \ "display_name"))

  /* Utility function to extract identifying documents */
  def getIdentifyingDocuments(jv: JValue): List[identifyingDocument] = (jv: @unchecked) match {
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
  def getLinks(id: String, jv: JValue): List[Link] = (jv: @unchecked) match {
    case JArray(List()) => List[Link]()
    case JArray(links) =>
      links.
        children.
        map(link => compact(render(link \ "is_reverse")) match {
          case "false" => Link(
            subjId = id,
            predicate = convertPredicate(compact(render(link \ "relation_type"))),
            objId = compact(render(link \ "linked_id")))
          case "true" => Link(
            subjId = compact(render(link \ "linked_id")),
            predicate = convertPredicate(compact(render(link \ "relation_type"))),
            objId = id)
      })
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
    val subtype = listing \ "party_sub_type"

    /* Extract ID documents */
    val idDocs: List[Entity] = getIdentifyingDocuments(listing \ "documents").
      map(idDoc => Entity(idDoc.number, idDoc)).toList
    val idDocLinks: List[Link] = idDocs.
      map(idDoc => Link(id, "HAS_ID_DOC", idDoc.id)).toList

    /* Determine entity type */
    val proto = (subtype: @unchecked) match {
      case JString("Entity") =>
        organization(name)
      case JString("Individual") =>
        person(name)
      case JString("Vessel") =>
        vessel(name)
      case JString("Aircraft") =>
        aircraft(name)
    }

    /* Generate partial graph */
    val `entities` = Entity(id, proto) :: idDocs
    val `links` = getLinks(id, listing \ "linked_profiles") ::: idDocLinks
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
