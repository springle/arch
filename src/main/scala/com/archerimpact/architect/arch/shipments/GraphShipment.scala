package com.archerimpact.architect.arch.shipments

import com.archerimpact.architect.ontology.source
import scalapb.descriptors.PMessage

object GraphShipment {

  /* Enforce global uniqueness on entities */
  def globalEntities(entities: List[Entity], url: String): List[Entity] =
    entities.map(entity => entity.copy(id = s"$url%${entity.id}"))

  /* Enforce global uniqueness on links */
  def globalLinks(links: List[Link], url: String): List[Link] =
    links.map(link => link.copy(subjId = s"$url%${link.subjId}", objId = s"$url%${link.objId}"))

  def apply(entities: List[Entity], links: List[Link], url: String): GraphShipment = {
    val percentUrl = url.replace("/","%")
    new GraphShipment(
      globalEntities(entities, percentUrl),
      globalLinks(links, percentUrl),
      percentUrl
    )
  }
}

case class GraphShipment(entities: List[Entity], links: List[Link], url: String) extends Shipment
case class Entity(id: String, proto: scalapb.GeneratedMessage)
case class Link(subjId: String, predicate: String, objId: String)