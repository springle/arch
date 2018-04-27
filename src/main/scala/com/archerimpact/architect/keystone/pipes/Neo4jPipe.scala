package com.archerimpact.architect.keystone.pipes

import com.archerimpact.architect.keystone._
import com.archerimpact.architect.keystone.shipments.GraphShipment


class Neo4jPipe extends PipeSpec {

  override type InType = GraphShipment
  override type OutType = GraphShipment

  private val neo4jSession = newNeo4jSession()

  def clean(s: Any): String = s.toString.replace("'", "")

  def uploadLinks(graph: GraphShipment): Unit =
    for (link <- graph.links) neo4jSession.run(
      s"MATCH " +
        s"(subj:${typeName(link.subj.proto)} {architectId:'${architectId(link.subj, graph)}'}), " +
        s"(obj:${typeName(link.obj.proto)} {architectId:'${architectId(link.obj, graph)}'})\n" +
        s"CREATE (subj)-[:${link.predicate}]->(obj)"
    )

  def uploadEntities(graph: GraphShipment): Unit =
    for (entity <- graph.entities) neo4jSession.run(
        s"CREATE (entity:${typeName(entity.proto)} {" + s"architectId:'${architectId(entity, graph)}'," +
          protoParams(entity.proto).map { case (k, v) => k + s":'${clean(v)}'" }.mkString(",") + "})"
    )

  override def flow(input: GraphShipment): GraphShipment = {
    uploadEntities(input); uploadLinks(input); input
  }

}
