package com.archerimpact.architect.arch.parsers.countries.testdata

import com.archerimpact.architect.arch.parsers.formats.JSONParser
import com.archerimpact.architect.arch.shipments.{Entity, GraphShipment, Link}
import com.archerimpact.architect.ontology._
import org.json4s._
import org.json4s.JsonDSL._
import org.json4s.native.JsonMethods._

case class PartialGraph(entities: List[Entity] = List[Entity](), links: List[Link] = List[Link]())

class ep_votes_trim extends JSONParser {

  implicit val formats: DefaultFormats.type = DefaultFormats

  /* Utility function to extract title */
  def getTitle(jv: JValue): String = compact(render(jv \ "title"))

  /* Utility function to extract links */
  def getVotes(id: String, jv: JValue, predicate: String): PartialGraph = jv match {
    case JArray(List()) => PartialGraph()
    case JArray(groups) =>
      val links = for {
        group <- groups.children
        votes = group \ "votes"
        vote <- votes.children
      } yield {
        Link(
          subjId = (vote \ "orig").extract[String],
          predicate = predicate,
          objId = id
        )
      }
      val entities = links.map(link => Entity(link.subjId, person(link.subjId)))
      PartialGraph(entities, links)
  }

  def getPartialGraph(listing: JValue): PartialGraph = {

    /* Extract fields */
    val id = compact(render(getTitle(listing).substring(0, 12)))
    val title = getTitle(listing)

    val proto = vote(title)

    /* Generate partial graph */
    val `entities` = PartialGraph(List(Entity(id, proto)), List[Link]())
    val `for` = getVotes(id, listing \ "For" \ "groups", "VOTED_FOR")
    val `against` = getVotes(id, listing \ "Against" \ "groups", "VOTED_AGAINST")
    val `abstain` = getVotes(id, listing \ "Abstain" \ "groups", "ABSTAINED_ON")
    val partialGraphs = List(`entities`, `for`, `against`, `abstain`)
    partialGraphs.foldLeft(PartialGraph())(merge)
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
