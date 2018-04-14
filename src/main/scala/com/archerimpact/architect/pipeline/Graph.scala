package com.archerimpact.architect.pipeline

case class Graph(entities: List[Entity], links: List[Link])
case class Entity(name: String, uid: String)
case class Link(subj: Entity, pred: String, obj: Entity)
