package com.archerimpact.architect.keystone

case class Graph(entities: List[Entity], links: List[Link], url: String)
case class Entity(name: String, uid: String)
case class Link(subj: Entity, pred: String, obj: Entity)
