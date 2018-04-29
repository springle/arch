package com.archerimpact.architect.keystone.pipes

import com.archerimpact.architect.keystone._
import com.archerimpact.architect.keystone.shipments.GraphShipment


class Neo4jPipe extends PipeSpec {

  override type InType = GraphShipment
  override type OutType = GraphShipment

  private val neo4jSession = newNeo4jSession()

  def clean(s: Any): String = s.toString.replace("'", "")

  def uploadLinks(graph: GraphShipment): Unit =
    for (link <- graph.links)


  def uploadEntities(graph: GraphShipment): Unit =
    neo4jSession.run(
      graph.entities.
        map(entity => s"CREATE (entity:${typeName(entity.proto)} {" + s"architectId:'${entity.id}'})").
        mkString("\n")
    )

  override def flow(input: GraphShipment): GraphShipment = {
    uploadEntities(input); uploadLinks(input); input
  }

}
