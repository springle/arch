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

case class GraphShipment(entities: List[Entity], links: List[Link], url: String, source: String) extends Shipment
case class Entity(id: String, proto: scalapb.GeneratedMessage)
case class Link(subjId: String, predicate: String, objId: String)