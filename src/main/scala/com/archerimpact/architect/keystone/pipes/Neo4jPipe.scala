package com.archerimpact.architect.keystone.pipes

import com.archerimpact.architect.keystone._
import com.archerimpact.architect.keystone.shipments.GraphShipment


class Neo4jPipe extends PipeSpec {

  override type InType = GraphShipment
  override type OutType = GraphShipment

  private val neo4jSession = newNeo4jSession()
  private val GROUP_SIZE = 200

  def clean(s: Any): String = s.toString.replace("'", "").replace(".","")

  /*
    graph.links.
      map(link => s"MATCH (subject),(object) " +
        s"WHERE subject.architectId = '${link.subjId}' " +
        s"AND object.architectId = '${link.objId}' " +
        s"MERGE (subject)-[:${link.predicate}]->(object)").
      foreach(script => neo4jSession.run(script))
  */

  def uploadLinks(graph: GraphShipment): Unit = {
    println(graph.links.size)
    graph.
      links.
      grouped(GROUP_SIZE).
      toList.
      foreach(group => neo4jSession.run(
        group.
          map(link => s"MATCH (`${link.subjId}`),(`${link.objId}`) " +
            s"WHERE `${link.subjId}`.architectId = '${link.subjId}' " +
            s"AND `${link.objId}`.architectId = '${link.objId}' " +
            s"MERGE (`${link.subjId}`)-[:${link.predicate}]->(`${link.objId}`)").
          mkString("\n")
      ))
  }

  def uploadEntities(graph: GraphShipment): Unit = {
    println(graph.entities.size)
    graph.
      entities.
      grouped(GROUP_SIZE).
      toList.
      foreach(group => neo4jSession.run(
        group.
          map(entity => s"MERGE (`${entity.id}`:${typeName(entity.proto)} " +
            s"{architectId:'${entity.id}', firstField:'${clean(entity.proto.getFieldByNumber(1))}'})").
          mkString("\n")
      ))
  }

  def createConstraints(graph: GraphShipment): Unit =
    graph.
      entities.
      map(entity => typeName(entity)).
      foreach(entityType => neo4jSession.run(
      s"CREATE CONSTRAINT ON (entity:$entityType) ASSERT entity.architectId IS UNIQUE"
      ))

  override def flow(input: GraphShipment): GraphShipment = {
    createConstraints(input)
    uploadEntities(input)
    uploadLinks(input)
    input
  }

}
