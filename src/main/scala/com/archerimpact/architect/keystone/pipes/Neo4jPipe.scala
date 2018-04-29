package com.archerimpact.architect.keystone.pipes

import com.archerimpact.architect.keystone._
import com.archerimpact.architect.keystone.shipments.GraphShipment


class Neo4jPipe extends PipeSpec {

  override type InType = GraphShipment
  override type OutType = GraphShipment

  private val neo4jSession = newNeo4jSession()

  def clean(s: Any): String = s.toString.replace("'", "")

  def uploadLinks(graph: GraphShipment): Unit = None

  def uploadEntities(graph: GraphShipment): Unit =
    for (entity <- graph.entities) neo4jSession.run(
        s"CREATE (entity:${typeName(entity.proto)} {" + s"architectId:'${entity.id}'," +
          protoParams(entity.proto).map { case (k, v) => k + s":'${clean(v)}'" }.mkString(",") + "})"
    )

  override def flow(input: GraphShipment): GraphShipment = {
    uploadEntities(input); uploadLinks(input); input
  }

}
