package com.archerimpact.architect.keystone.sinks

import akka.actor.{ActorRef, Props}
import com.archerimpact.architect.keystone.{Entity, Graph, Link}
import org.neo4j.driver.v1.{Driver, GraphDatabase, Session}

object Neo4jSinkActor {
  def props(nextSink: Option[ActorRef]): Props = Props(new Neo4jSinkActor(nextSink))
}

class Neo4jSinkActor(nextSink: Option[ActorRef],
                     val host: String = "localhost",
                     val port: String = "7687") extends SinkActor(nextSink) {

  val driver: Driver = GraphDatabase.driver(s"bolt://$host/$port")
  val session: Session = driver.session

  def uploadEntity(entity: Entity, url: String): String = {
    val script = s"CREATE (entity:${entity.proto.getClass.getName.replace(".","")} " +
      "{" + s"architectId:'$url/${entity.id}'," + getProtoParams(entity.proto).map{case (k, v) => k +
      ":" + s"'${v.toString.replace("'","")}'"}.mkString(",") + "})"
    val hold = session.run(script) // TODO: replace .toString with nid-extractor
    "test"
  }

  override def sinkGraph(graph: Graph): Unit = {
    val entityToNeo4jID = graph.entities.map(entity => (entity, uploadEntity(entity, graph.url))).toMap
    graph.links.map(link => ???)
  }
}
