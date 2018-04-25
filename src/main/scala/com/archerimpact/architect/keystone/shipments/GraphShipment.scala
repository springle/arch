package com.archerimpact.architect.keystone.shipments

case class GraphShipment(entities: List[Entity], links: List[Link], url: String) extends Shipment
case class Entity(id: String, proto: AnyRef)
case class Link(subj: Entity, predicate: String, obj: Entity)