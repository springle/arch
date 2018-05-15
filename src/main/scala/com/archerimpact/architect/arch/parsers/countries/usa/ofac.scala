package com.archerimpact.architect.arch.parsers.countries.usa

import java.text.SimpleDateFormat

import com.archerimpact.architect.arch.parsers.formats.JSONParser
import com.archerimpact.architect.arch.shipments.{Entity, GraphShipment, Link}
import com.archerimpact.architect.ontology._
import org.json4s.JsonDSL._
import org.json4s._

case class PartialGraph(entities: List[Entity] = List[Entity](), links: List[Link] = List[Link]())

class ofac extends JSONParser {

  implicit val formats: DefaultFormats.type = DefaultFormats
  private val inputDateFormat = new SimpleDateFormat("yyyy-mm-dd")
  private val outputDateFormat = new SimpleDateFormat("yyyy-mm-dd")

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
  def getProtoForSubtype(subtype: String, name: String): scalapb.GeneratedMessage =
    (subtype: @unchecked) match {
      case "Entity"     => organization(name)
      case "Individual" => person(name)
      case "Vessel"     => vessel(name)
      case "Aircraft"   => aircraft(name)
    }

  /* Utility function to extract locations */
  def getLocations(jv: JValue): List[Entity] = jv match {
    case JArray(locations) =>
      for (location <- locations.children) yield {
        val line1 = (location \\ "ADDRESS1").extractOpt[String].getOrElse("")
        val line2 = (location \\ "ADDRESS2").extractOpt[String].getOrElse("")
        val line3 = (location \\ "ADDRESS3").extractOpt[String].getOrElse("")
        val city = (location \\ "CITY").extractOpt[String].getOrElse("")
        val region = (location \\ "STATE/PROVINCE").extractOpt[String].getOrElse("")
        val zipCode = (location \\ "POSTAL CODE").extractOpt[String].getOrElse("")
        val country = (location \\ "COUNTRY").extractOpt[String].getOrElse("")
        val combined = List(line1, line2, line3, city, region, zipCode, country).mkString(",")
        Entity(combined, address(combined, line1, line2, line3, city, region, zipCode, country))
      }
    case _ => List[Entity]()
  }

  /* Utility function to extract sanction events */
  def getSanctionEvents(jv: JValue): List[Entity] = jv match {
    case JArray(sanctionEntries) =>
      for (sanctionEntry <- sanctionEntries.children) yield {
        val program = (sanctionEntry \\ "program")(0).extract[String]
        val entryEvent = (sanctionEntry \\ "entry_events")(0)
        val date = inputDateFormat.parse(entryEvent(0).extract[String])
        val description = entryEvent(1).extract[String]
        Entity(s"${inputDateFormat.format(date)}/$description", event(
          description = description,
          date = outputDateFormat.format(date),
          category = event.Category.SANCTION,
          group = program
        ))
      }
    case _ => List[Entity]()
  }

  /* Utility function to extract identifying documents */
  def getIdentifyingDocuments(jv: JValue, id: String): List[Entity] = jv match {
    case JArray(documents) =>
      for (document <- documents.children) yield {
        val number = (document \ "id_number").extract[String]
        val numberType = (document \ "type").extract[String]
        val issuedBy = (document \ "issued_by").extract[String]
        val issuedIn = (document \ "issued_in").extract[String]
        val valid = document \ "validity" == JString("Valid")
        Entity(s"$id/idDoc/$number", identifyingDocument(number, numberType, issuedBy, issuedIn, valid))
      }
    case _ => List[Entity]()
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
    val idDocs = getIdentifyingDocuments(listing \ "documents", id)
    val idDocLinks = idDocs.map(idDoc => Link(id, "HAS_ID_DOC", idDoc.id))

    /* Extract aliases */
    val aliases = getAliases(listing \\ "aliases", subtype, id)
    val aliasLinks = aliases.map(alias => Link(id, "AKA", alias.id))

    /* Extract sanction events */
    val sanctionEvents = getSanctionEvents(listing \\ "sanctions_entries")
    val sanctionLinks = sanctionEvents.map(sanctionEvent => Link(id, "SANCTIONED_ON", sanctionEvent.id))

    /* Extract locations */
    val locations = getLocations(listing \\ "Location")
    val locationLinks = locations.map(location => Link(id, "HAS_KNOWN_LOCATION", location.id))

    /* Extract official links */
    val officialLinks = getLinks(id, listing \ "linked_profiles")

    /* Generate primary entity for listing */
    val primaryEntity = Entity(id, proto)

    /* Generate partial graph */
    val entities = primaryEntity :: idDocs ::: aliases ::: sanctionEvents ::: locations
    val links = officialLinks ::: idDocLinks ::: aliasLinks ::: sanctionLinks ::: locationLinks
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
