package com.archerimpact.architect.keystone.shipments

import scalapb.Message
import scalapb.lenses.Updatable

/* Graph */

object GraphShipment {
  def apply(entities: List[Entity], links: List[Link], url: String): GraphShipment =
    new GraphShipment(entities.map(e => e.copy(id=s"$url/${e.id}")),
      links.map(l => l.copy(subjId = s"$url/${l.subjId}", objId = s"$url/${l.objId}")), url)
}

case class GraphShipment(entities: List[Entity], links: List[Link], url: String) extends Shipment

/* Entities */

case class Entity(id: String, proto: scalapb.GeneratedMessage with Message[T] with Updatable[T] forSome {type T} )

/* Links */

case class Link(subjId: String, predicate: String, objId: String)