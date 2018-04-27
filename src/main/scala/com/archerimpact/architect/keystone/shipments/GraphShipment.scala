package com.archerimpact.architect.keystone.shipments

/* Graph */

object GraphShipment {

  def merge(id: String, group: List[Entity]): Entity = {
    require(group.forall(entity => entity.proto.getClass.getName == group.last.proto.getClass.getName))
  }

  def resolveEntities(entities: List[Entity]): List[Entity] =
    entities.groupBy(e=>e.id).map {
      case (id, group) => merge(id, group)
    }.toList


  def apply(entities: List[Entity], links: List[Link], url: String): GraphShipment =
    new GraphShipment(entities.map(e => e.copy(id=s"$url/${e.id}")), links, url)
}

case class GraphShipment(entities: List[Entity], links: List[Link], url: String) extends Shipment {

}

/* Entities */

case class Entity(id: String, proto: AnyRef)

/* Links */

case class Link(subj: Entity, predicate: String, obj: Entity)