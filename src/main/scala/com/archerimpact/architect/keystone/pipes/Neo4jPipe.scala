package com.archerimpact.architect.keystone.pipes

import akka.actor.{ActorRef, Props}
import com.archerimpact.architect.keystone._
import com.archerimpact.architect.keystone.shipments.{GraphShipment, Shipment}

object Neo4jPipe {
  def props(nextPipes: List[ActorRef]): Props = Props(new Neo4jPipe(nextPipes))
}

class Neo4jPipe(nextPipes: List[ActorRef]) extends PipeActor(nextPipes) {

  private val neo4jSession = newNeo4jSession()

  def clean(s: Any): String = s.toString.replace("'", "")

  def uploadLinks(graph: GraphShipment): Unit =
    for (link <- graph.links) neo4jSession.run(
      s"MATCH " +
        s"(subj:${protoType(link.subj.proto)} {architectId:'${architectId(link.subj, graph)}'}), " +
        s"(obj:${protoType(link.obj.proto)} {architectId:'${architectId(link.obj, graph)}'})\n" +
        s"CREATE (subj)-[:${link.predicate}]->(obj)"
    )

  def uploadEntities(graph: GraphShipment): Unit =
    for (entity <- graph.entities) neo4jSession.run(
        s"CREATE (entity:${protoType(entity.proto)} {" + s"architectId:'${architectId(entity, graph)}'," +
          protoParams(entity.proto).map { case (k, v) => k + s":'${clean(v)}'" }.mkString(",") + "})"
    )

  override def processShipment(shipment: Shipment): GraphShipment = shipment match {
    case graph: GraphShipment => uploadEntities(graph); uploadLinks(graph); graph
  }

  override def updateStats(): Unit = context.parent ! KeystoneSupervisor.IncToNeo4j

}
