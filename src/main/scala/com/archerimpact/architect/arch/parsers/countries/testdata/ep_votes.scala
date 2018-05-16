package com.archerimpact.architect.arch.parsers.countries.testdata

import com.archerimpact.architect.arch.parsers.formats.JSONParser
import com.archerimpact.architect.arch.shipments.{Entity, GraphShipment, Link}
import com.archerimpact.architect.ontology._
import org.json4s._
import org.json4s.JsonDSL._
import org.json4s.native.JsonMethods._

// case class PartialGraph(entities: List[Entity] = List[Entity](), links: List[Link] = List[Link]())

class epVotes extends JSONParser {

  /* Utility function to extract title */
  def getTitle(jv: JValue): String = compact(render(jv \ "title"))

  /* Utility function to extract links */
  def getFor(id: String, jv: JValue): List[Link] = jv match {
    case JArray(List()) => List[Link]()
    case JArray(links) =>
      links.
        children.
        map(link =>
          Link(
            subjId = compact(render(link \ "groups" \ "votes" \ "orig")),
            predicate = "VOTED_FOR",
            objId = id)
          )
  }

  def getAgainst(id: String, jv: JValue): List[Link] = jv match {
    case JArray(List()) => List[Link]()
    case JArray(links) =>
      links.
        children.
        map(link =>
          Link(
            subjId = compact(render(link \ "groups" \ "votes" \ "orig")),
            predicate = "VOTED_AGAINST",
            objId = id)
          )
  }

  def getAbstain(id: String, jv: JValue): List[Link] = jv match {
    case JArray(List()) => List[Link]()
    case JArray(links) =>
      links.
        children.
        map(link =>
          Link(
            subjId = compact(render(link \ "groups" \ "votes" \ "orig")),
            predicate = "ABSTAINED_ON",
            objId = id)
          )
  }

  /* Utility function to transform predicate names */
  def convertPredicate(predicate: String): String = predicate match {
    case `predicate` if predicate.contains("For") => "VOTED_FOR"
    case `predicate` if predicate.contains("Against") => "VOTED_AGAINST"
    case `predicate` if predicate.contains("Abstain") => "ABSTAINED_ON"
  }

  def getPartialGraph(listing: JValue): PartialGraph = {

    /* Extract fields */
    val id = compact(render(getTitle(listing).substring(0, 12)))
    val title = getTitle(listing)

    val proto = vote(title)

    /* Generate partial graph */
    val `entities` = Entity(id, proto)
    val `for` = getFor(id, listing \ "For" \ "groups")
    val `against` = getAgainst(id, listing \ "Against" \ "groups")
    val `abstain` = getAbstain(id, listing \ "Abstain" \ "groups")
    PartialGraph(List(`entities`), `for` ::: `against` ::: `abstain`)
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
