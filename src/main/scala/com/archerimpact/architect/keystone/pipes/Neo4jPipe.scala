package com.archerimpact.architect.keystone.pipes

import akka.actor.{ActorRef, Props}
import com.archerimpact.architect.keystone._
import com.archerimpact.architect.keystone.shipments.{GraphShipment, Shipment}
import org.neo4j.driver.v1.{Driver, GraphDatabase, Session}


object Neo4jPipe {
  def props(nextPipes: List[ActorRef]): Props = Props(new Neo4jPipe(nextPipes))
}

class Neo4jPipe(nextPipes: List[ActorRef]) extends PipeActor(nextPipes) {

  private val host = scala.util.Properties.envOrElse("NEO4J_HOST", "localhost")
  private val port = scala.util.Properties.envOrElse("NEO4J_PORT", "7687")

  val driver: Driver = GraphDatabase.driver(s"bolt://$host/$port")
  val session: Session = driver.session

  def clean(s: Any): String = s.toString.replace("'", "")

  def uploadLinks(graph: GraphShipment): Unit =
    for (link <- graph.links) session.run(
      s"MATCH " +
        s"(subj:${getProtoType(link.subj.proto)} {architectId:'${architectId(link.subj, graph)}'}), " +
        s"(obj:${getProtoType(link.obj.proto)} {architectId:'${architectId(link.obj, graph)}'})\n" +
        s"CREATE (subj)-[:${link.predicate}]->(obj)"
    )

  def uploadEntities(graph: GraphShipment): Unit =
    for (entity <- graph.entities) session.run(
        s"CREATE (entity:${getProtoType(entity.proto)} {" + s"architectId:'${architectId(entity, graph)}'," +
          getProtoParams(entity.proto).map { case (k, v) => k + s":'${clean(v)}'" }.mkString(",") + "})"
    )

  override def processShipment(shipment: Shipment): GraphShipment = shipment match {
    case graph: GraphShipment => uploadEntities(graph); uploadLinks(graph); graph
  }

  override def updateStats(): Unit = context.parent ! KeystoneSupervisor.IncToNeo4j

}
