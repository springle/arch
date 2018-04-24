package com.archerimpact.architect.keystone.shipments

case class Graph(entities: List[Entity], links: List[Link], url: String) extends Shipment
case class Entity(id: String, proto: Any)
case class Link(subj: Entity, predicate: String, obj: Entity)