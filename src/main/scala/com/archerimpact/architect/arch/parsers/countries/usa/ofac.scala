package com.archerimpact.architect.arch.parsers.countries.usa

import com.archerimpact.architect.arch.parsers.formats.JSONParser
import com.archerimpact.architect.arch.shipments.{Entity, GraphShipment, Link}
import com.archerimpact.architect.ontology._
import org.json4s.JsonDSL._
import org.json4s._

case class PartialGraph(entities: List[Entity] = List[Entity](), links: List[Link] = List[Link]())

class ofac extends JSONParser {

  implicit val formats: DefaultFormats.type = DefaultFormats

  /* Utility function to extract name */
  def getName(jv: JValue): String = (jv \ "identity" \ "primary" \ "display_name").extract[String]

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

  /* Utility function to create basic protobuf of a variable type (for aliases, etc.) */
  def getProtoForSubtype(subtype: String, name: String): scalapb.GeneratedMessage = (subtype: @unchecked) match {
    case "Entity"     => organization(name)
    case "Individual" => person(name)
    case "Vessel"     => vessel(name)
    case "Aircraft"   => aircraft(name)
  }

  /* Utility function to extract identifying documents */
  def getIdentifyingDocuments(jv: JValue): List[identifyingDocument] = (jv: @unchecked) match {
    case JArray(List()) => List[identifyingDocument]()
    case JArray(documents) =>
      documents.children.map(document => identifyingDocument(
        number = (document \ "id_number").extract[String],
        numberType = (document \ "type").extract[String],
        issuedBy = (document \ "issued_by").extract[String],
        issuedIn = (document \ "issued_in").extract[String],
        valid = document \ "validity" == JString("Valid")
      ))
  }

  /* Utility function to get aliases for an entity */
  def getAliases(jv: JValue, subtype: String, id: String): List[Entity] = jv match {
    case JArray(aliases) =>
      aliases.children.map(alias => Entity(
        id = s"$id/aka/" + (alias \\ "display_name").extract[String],
        proto = getProtoForSubtype(subtype, (alias \\ "display_name").extract[String])
      ))
    case _ => List[Entity]()
  }

  /* Utility function to extract links */
  def getLinks(id: String, jv: JValue): List[Link] = (jv: @unchecked) match {
    case JArray(List()) => List[Link]()
    case JArray(links) =>
      links.
        children.
        map(link =>
          if ((link \ "is_reverse").extract[Boolean])
            Link(
              subjId = (link \ "linked_id").extract[String],
              predicate = convertPredicate((link \ "relation_type").extract[String]),
              objId = id)
          else
            Link(
              subjId = id,
              predicate = convertPredicate((link \ "relation_type").extract[String]),
              objId = (link \ "linked_id").extract[String])
        )
  }

  /*
   *    Main function to run on each entry in the OFAC JSON.
   *    Returns a PartialGraph with a list of entities and a list of links.
   *    These partial graphs will be merged together to create the full GraphShipment.
   */
  def getPartialGraph(listing: JValue): PartialGraph = {

    /* Extract fields */
    val id = (listing \ "fixed_ref").extract[String]
    val name = getName(listing)
    val subtype = (listing \ "party_sub_type").extract[String]

    /* Determine entity type */
    val proto = (subtype: @unchecked) match {
      case "Entity"     => organization(name)
      case "Individual" => person(name)
      case "Vessel"     => vessel(name)
      case "Aircraft"   => aircraft(name)
    }

    /* Extract ID documents */
    val idDocs: List[Entity] =
      getIdentifyingDocuments(listing \ "documents").
      map(idDoc => Entity(id + "/idDoc/" + idDoc.number, idDoc))
    val idDocLinks: List[Link] =
      idDocs.
      map(idDoc => Link(id, "HAS_ID_DOC", idDoc.id))

    /* Extract aliases */
    val aliases = getAliases(listing \\ "aliases", subtype, id)
    val aliasLinks = aliases.map(alias => Link(id, "AKA", alias.id))

    /* Extract official links */
    val officialLinks = getLinks(id, listing \ "linked_profiles")

    /* Generate primary entity for listing */
    val primaryEntity = Entity(id, proto)

    /* Generate partial graph */
    val entities = primaryEntity :: idDocs ::: aliases
    val links = officialLinks ::: idDocLinks ::: aliasLinks
    PartialGraph(entities, links)
  }

  /* Utility function to merge partial graphs */
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
