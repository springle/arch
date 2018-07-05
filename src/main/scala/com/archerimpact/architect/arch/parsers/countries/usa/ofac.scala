package com.archerimpact.architect.arch.parsers.countries.usa

import java.text.SimpleDateFormat

import com.archerimpact.architect.arch.parsers.formats.JSONParser
import com.archerimpact.architect.arch.shipments.{Entity, GraphShipment, Link, PartialGraph}
import com.archerimpact.architect.ontology._
import org.json4s.JsonDSL._
import org.json4s._

class ofac extends JSONParser {

  val source = "OFAC SDN List"

  implicit val formats: DefaultFormats.type = DefaultFormats
  private val inputDateFormat = new SimpleDateFormat("yyyy-mm-dd")

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
      val addresses = for (location <- locations.children) yield {
        val line1 = (location \\ "ADDRESS1").extractOpt[String].getOrElse("")
        val line2 = (location \\ "ADDRESS2").extractOpt[String].getOrElse("")
        val line3 = (location \\ "ADDRESS3").extractOpt[String].getOrElse("")
        val city = (location \\ "CITY").extractOpt[String].getOrElse("")
        val region = (location \\ "STATE/PROVINCE").extractOpt[String].getOrElse("")
        val zipCode = (location \\ "POSTAL CODE").extractOpt[String].getOrElse("")
        val country = (location \\ "COUNTRY").extractOpt[String].getOrElse("")
        val combined = List(line1, line2, line3, city, region, zipCode, country).
          filter(s => s != "").
          map(s => s.stripPrefix(" ")).
          mkString(", ")
        Entity(combined, address(combined, line1, line2, line3, city, region, zipCode, country))
      }
      addresses.
        filter(address => Option(address.proto.getFieldByNumber(1)).
          getOrElse("").
          toString.
          count(c => c == ',') > 1)  // filter out non-specific addresses
    case _ => List[Entity]()
  }

  /* Utility function to extract sanction events */
  def getSanctionEvents(jv: JValue): List[Entity] = jv match {
    case JArray(sanctionEntries) =>
      for {
        sanctionEntry <- sanctionEntries.children
        entryEvent <- (sanctionEntry \ "entry_events").children
      } yield {
        val date = inputDateFormat.parse(entryEvent(0).extract[String])
        val description = entryEvent(1).extract[String]
        Entity(s"${inputDateFormat.format(date)}/$description", event(
          description = description,
          date = inputDateFormat.format(date),
          category = event.Category.SANCTION
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
  def getAliases(jv: JValue, subtype: String, id: String): List[String] = jv match {
    case JArray(aliases) =>
      aliases.children.map(alias => (alias \\ "display_name").extract[String])
    case _ => List[String]()
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

  /* Utility function to extract details */
  def getDetails(listing: JValue, targetObject: String, targetDetails: String = "details"): List[String] =
    for {
      item <- (listing \\ targetObject).children
      details = (item \\ targetDetails).extractOpt[String].getOrElse("")
      if details != ""
    } yield details

  /*
   *    Main function to run on each entry in the OFAC JSON.
   *    Returns a PartialGraph with a list of entities and a list of links.
   *    These partial graphs will be merged together to create the full GraphShipment.
   */
  def getPartialGraph(listing: JValue): PartialGraph = {

    /* Extract fields */
    val id: String = (listing \ "fixed_ref").extract[String]
    val name: String = (listing \ "identity" \ "primary" \ "display_name").extract[String]
    val dateOfBirth: List[String] = getDetails(listing, "Birthdate", "date")
    val placeOfBirth: List[String] = getDetails(listing, "Place of Birth")
    val titles: List[String] = getDetails(listing, "Title")
    val emailAddresses: List[String] = getDetails(listing, "Email Address")
    val websites: List[String] = getDetails(listing, "Website")
    val subtype: String = (listing \ "party_sub_type").extract[String]
    val aliases: List[String] = getAliases(listing \\ "aliases", subtype, id)

    /* Determine entity type */
    val proto = (subtype: @unchecked) match {
      case "Entity"     => organization(name, emailAddresses, websites, aliases)
      case "Individual" => person(name, dateOfBirth, placeOfBirth, titles, emailAddresses, websites, aliases)
      case "Vessel"     => vessel(name)
      case "Aircraft"   => aircraft(name)
    }

    /* Extract ID documents */
    val idDocs = getIdentifyingDocuments(listing \ "documents", id)
    val idDocLinks = idDocs.map(idDoc => Link(id, "HAS_ID_DOC", idDoc.id))

    /* Extract aliases */
    //val aliases = getAliases(listing \\ "aliases", subtype, id)
    //val aliasLinks = aliases.map(alias => Link(id, "AKA", alias.id))

    /* Extract sanction events */
    val sanctionEvents = getSanctionEvents(listing \\ "sanctions_entries")
    val sanctionLinks = sanctionEvents.map(sanctionEvent => Link(id, "SANCTIONED_ON", sanctionEvent.id))

    /* Extract locations (filter out empties) */
    val locations = getLocations(listing \\ "Location")
    val locationLinks = locations.map(location => Link(id, "HAS_KNOWN_LOCATION", location.id))

    /* Extract official links */
    val officialLinks = getLinks(id, listing \ "linked_profiles")

    /* Generate primary entity for listing */
    val primaryEntity = Entity(id, proto)

    /* Generate partial graph */
    val entities = primaryEntity :: idDocs ::: sanctionEvents ::: locations
    val links = officialLinks ::: idDocLinks ::: sanctionLinks ::: locationLinks
    PartialGraph(entities, links)
  }

  override def jsonToGraph(data: JValue, url: String): GraphShipment = {

    /* Generate partial graph for each sanction listing */
    val partialGraphs = data.children.map(listing => getPartialGraph(listing))

    /* Merge into a single graph to return */
    val mergedGraph = partialGraphs.foldLeft(PartialGraph())(PartialGraph.merge)
    GraphShipment(mergedGraph.entities, mergedGraph.links, url, source)
  }
}
