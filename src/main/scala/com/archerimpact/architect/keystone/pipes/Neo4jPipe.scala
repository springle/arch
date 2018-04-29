package com.archerimpact.architect.keystone.pipes

import com.archerimpact.architect.keystone._
import com.archerimpact.architect.keystone.shipments.GraphShipment


class Neo4jPipe extends PipeSpec {

  override type InType = GraphShipment
  override type OutType = GraphShipment

  private val neo4jSession = newNeo4jSession()

  def clean(s: Any): String = s.toString.replace("'", "")

  def uploadLinks(graph: GraphShipment): Unit =
    graph.links.
      map(link => s"MATCH (subject),(object) " +
        s"WHERE subject.architectId = '${link.subjId}' " +
        s"AND object.architectId = '${link.objId}' " +
        s"MERGE (subject)-[:${link.predicate}]->(object)").
      foreach(script => neo4jSession.run(script))

  def uploadEntities(graph: GraphShipment): Unit =
    graph.entities.
      map(entity => s"MERGE (entity:${typeName(entity.proto)} {" + s"architectId:'${entity.id}'})").
      foreach(script => neo4jSession.run(script))

  override def flow(input: GraphShipment): GraphShipment = {
    uploadEntities(input); println("uploaded entities")
    uploadLinks(input); println("uploaded links")
    input
  }

}
