package com.archerimpact.architect.keystone.shipments

import com.archerimpact.architect.ontology.source
import scalapb.descriptors.PMessage

object GraphShipment {

  /* Add source entity and enforce global uniqueness */
  def updateEntities(entities: List[Entity], url: String): List[Entity] =
    Entity(url, source(
      url = url,
      author = "archer",
      investigation = "architect"
    )) :: entities.map(entity => entity.copy(id = s"$url/${entity.id}"))

  /* Add links to source entity and enforce global uniqueness */
  def updateLinks(entities: List[Entity], links: List[Link], url: String): List[Link] = {
    val updatedLinks = links.map(link => link.copy(subjId = s"$url/${link.subjId}", objId = s"$url/${link.objId}"))
    updatedLinks ::: entities.map(entity => Link(s"$url/${entity.id}", "APPEARS_ON", url))
  }

  def apply(entities: List[Entity], links: List[Link], url: String): GraphShipment =
    new GraphShipment(updateEntities(entities, url), updateLinks(entities, links, url), url)
}

case class GraphShipment(entities: List[Entity], links: List[Link], url: String) extends Shipment
case class Entity(id: String, proto: scalapb.GeneratedMessage)
case class Link(subjId: String, predicate: String, objId: String)