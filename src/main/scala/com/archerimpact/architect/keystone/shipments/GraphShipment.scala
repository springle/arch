package com.archerimpact.architect.keystone.shipments

object GraphShipment {
  def apply(entities: List[Entity], links: List[Link], url: String): GraphShipment =
    new GraphShipment(entities.map(e => e.copy(id=s"$url/${e.id}")), links, url)
}

case class GraphShipment(entities: List[Entity], links: List[Link], url: String) extends Shipment
case class Entity(id: String, proto: AnyRef)
case class Link(subj: Entity, predicate: String, obj: Entity)