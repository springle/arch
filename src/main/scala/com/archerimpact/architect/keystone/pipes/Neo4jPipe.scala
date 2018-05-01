package com.archerimpact.architect.keystone.pipes

import com.archerimpact.architect.keystone._
import com.archerimpact.architect.keystone.shipments.GraphShipment
import org.neo4j.driver.v1.{Session, Transaction}


class Neo4jPipe extends PipeSpec {

  override type InType = GraphShipment
  override type OutType = GraphShipment

  private val GROUP_SIZE = 1000

  def clean(s: Any): String = s.toString.replace("'", "").replace(".","")

  def uploadLinks(graph: GraphShipment, neo4jSession: Session): Unit =
    for (group <- graph.links.grouped(GROUP_SIZE)) {
      val tx: Transaction = neo4jSession.beginTransaction()
      tx.run(
        group.
          map(link => s"MATCH (`${link.subjId}`),(`${link.objId}`) " +
            s"WHERE `${link.subjId}`.architectId = '${link.subjId}' " +
            s"AND `${link.objId}`.architectId = '${link.objId}' " +
            s"MERGE (`${link.subjId}`)-[:${link.predicate}]->(`${link.objId}`) ").
          mkString("\n")
      )
      tx.success()
      tx.close()
    }

  def uploadEntities(graph: GraphShipment, neo4jSession: Session): Unit =
    for (group <- graph.entities.grouped(GROUP_SIZE)) {
      val tx: Transaction = neo4jSession.beginTransaction()
      tx.run(
        group.
          map(entity => s"MERGE (`${entity.id}`:${typeName(entity.proto)} " +
            s"{architectId:'${entity.id}', firstField:'${clean(entity.proto.getFieldByNumber(1))}'})").
          mkString("\n")
      )
      tx.success()
      tx.close()
    }

  override def flow(input: GraphShipment): GraphShipment = {
    val neo4jSession = newNeo4jSession()
    uploadEntities(input, neo4jSession); println("uploaded entities")
    uploadLinks(input, neo4jSession); println("uploaded links")
    neo4jSession.close()
    input
  }

}
