package com.archerimpact.architect.keystone.shipments

import com.archerimpact.architect.keystone

/* Graph */

object GraphShipment {
  def apply(entities: List[Entity], links: List[Link], url: String): GraphShipment =
    new GraphShipment(entities.map(e => e.copy(id=s"$url/${e.id}")), links, url)
}

case class GraphShipment(entities: List[Entity], links: List[Link], url: String) extends Shipment

/* Entities */

case class Entity(id: String, proto: scalapb.GeneratedMessage)

/* Links */

case class Link(subj: Entity, predicate: String, obj: Entity)