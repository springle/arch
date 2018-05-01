package com.archerimpact.architect.keystone.pipes

import com.archerimpact.architect.keystone._
import com.archerimpact.architect.keystone.shipments.GraphShipment
import org.neo4j.driver.v1.{Session, Statement, Transaction}
import scala.collection.JavaConverters._
import java.util.{HashMap=>JMap}

class Neo4jPipe extends PipeSpec {

  override type InType = GraphShipment
  override type OutType = GraphShipment

  private val GROUP_SIZE = 10000

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
      val statement =
        s"""
            WITH {entities} AS entities
            UNWIND entities AS entity
            MERGE (e:`entity.typeOf` {architectId: entity.id, display: entity.display})
         """.stripMargin
      val parameters: JMap[String, AnyRef] = new JMap[String, AnyRef]()
      parameters.put("entities",
        group.map(entity =>
          Map(
            "typeOf" -> typeName(entity),
            "id" -> entity.id,
            "display" -> clean(entity.proto.getFieldByNumber(1))
          ).asJava
        ).asJava
      )
      neo4jSession.run(statement, parameters)
    }

  def createIndices(graph: GraphShipment, neo4jSession: Session): Unit =
    for (entityType <- graph.entities.map(entity => typeName(entity)).toSet[String])
      neo4jSession.run(
        s"CREATE INDEX ON :$entityType(architectId)"
      )

  override def flow(input: GraphShipment): GraphShipment = {
    val neo4jSession = newNeo4jSession()
    createIndices(input, neo4jSession); println("created indices")
    uploadEntities(input, neo4jSession); println("uploaded entities")
    uploadLinks(input, neo4jSession); println("uploaded links")
    neo4jSession.close()
    input
  }

}
