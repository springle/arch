package com.archerimpact.architect.arch.shipments

object GraphShipment {

  def global(id: String, url: String): String =
    s"$url/$id".replace("'","").replace("\"","").replace("\\","")

  /* Enforce global uniqueness on entities */
  def globalEntities(entities: List[Entity], url: String): List[Entity] =
    entities.map(entity => entity.copy(id = global(entity.id, url)))

  /* Enforce global uniqueness on links */
  def globalLinks(links: List[Link], url: String): List[Link] =
    links.map(link => link.copy(subjId = global(link.subjId, url), objId = global(link.objId, url)))

  def apply(entities: List[Entity], links: List[Link], url: String, source: String): GraphShipment = {
    new GraphShipment(
      globalEntities(entities, url),
      globalLinks(links, url),
      url,
      source
    )
  }
}

object PartialGraph {

  /* Helper function for mergePartialGraphs */
  def merge(a: PartialGraph, b: PartialGraph): PartialGraph =
    a.copy(entities = a.entities ::: b.entities, links = a.links ::: b.links)

  /* Utility function to merge partial graphs */
  def mergePartialGraphs(partialGraphs: List[PartialGraph], url: String, source: String): GraphShipment = {
      val mergedGraph = partialGraphs.foldLeft(PartialGraph())(merge)
      GraphShipment(mergedGraph.entities, mergedGraph.links, url, source)
  }
}

case class GraphShipment(entities: List[Entity], links: List[Link], url: String, source: String) extends Shipment
case class PartialGraph(entities: List[Entity] = List[Entity](), links: List[Link] = List[Link]())
case class Entity(id: String, proto: scalapb.GeneratedMessage)
case class Link(subjId: String, predicate: String, objId: String)