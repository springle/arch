package com.archerimpact.architect.keystone

import akka.actor.{ActorRef, Props}
import com.archerimpact.architect.keystone.shipments.{Graph, Shipment}
import org.neo4j.driver.v1.{Driver, GraphDatabase, Session}


object Neo4jPipe {
  def props(nextPipes: List[ActorRef]): Props = Props(new Neo4jPipe(nextPipes))
}

class Neo4jPipe(nextPipes: List[ActorRef]) extends PipeActor(nextPipes) {

  private val host = scala.util.Properties.envOrElse("NEO4J_HOST", "localhost")
  private val port = scala.util.Properties.envOrElse("NEO4J_PORT", "7687")

  val driver: Driver = GraphDatabase.driver(s"bolt://$host/$port")
  val session: Session = driver.session

  def uploadLinks(graph: Graph): Unit = {
    val queries = for (link <- graph.links) yield
      s"MATCH (subj:${link.subj.proto.getClass.getName.split("\\.").last} {architectId:'${graph.url}/${link.subj.id}'}), " +
      s"(obj:${link.obj.proto.getClass.getName.split("\\.").last} {architectId:'${graph.url}/${link.obj.id}'})\n" +
      s"CREATE (subj)-[:${link.predicate}]->(obj)"
    for (query <- queries)
      session.run(query)
  }

  def uploadEntities(graph: Graph): Unit = {
    val queries = for (entity <- graph.entities) yield
        s"CREATE (entity:${entity.proto.getClass.getName.split("\\.").last} " +
        "{" + s"architectId:'${graph.url}/${entity.id}'," + getProtoParams(entity.proto).map { case (k, v) => k +
        ":" + s"'${v.toString.replace("'", "")}'" }.mkString(",") + "})"
    for (query <- queries)
      session.run(query)
  }

  override def processShipment(shipment: Shipment): Shipment = shipment match {
    case graph: Graph => uploadEntities(graph); uploadLinks(graph); graph
  }

  override def updateStats(): Unit = context.parent ! KeystoneSupervisor.IncToNeo4j

}
