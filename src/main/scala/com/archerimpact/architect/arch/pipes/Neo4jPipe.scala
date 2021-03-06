package com.archerimpact.architect.arch.pipes

import java.util.{HashMap => JMap}

import com.archerimpact.architect.arch._
import com.archerimpact.architect.arch.shipments.GraphShipment
import org.neo4j.driver.v1.{Session, Transaction}

import scala.collection.JavaConverters._

class Neo4jPipe extends PipeSpec {

  override type InType = GraphShipment
  override type OutType = GraphShipment
  val GROUP_SIZE = 1000

  /* Upload links to neo4j, grouped by predicate type */
  def uploadLinks(graph: GraphShipment, neo4jSession: Session): Unit = {
    for (group <- graph.links.grouped(GROUP_SIZE)) {
      val tx: Transaction = neo4jSession.beginTransaction()
      for (link <- group)
        tx.run(
          s"""
              MATCH (subject)
              WHERE subject.architectId = '${link.subjId}'
              WITH subject
              MATCH (object)
              WHERE object.architectId = '${link.objId}'
              MERGE (subject)-[:${link.predicate}]->(object)
            """.stripMargin
        )

      tx.success()
      tx.close()
    }

  }

  /* Upload entities to neo4j, grouped by proto type */
  def uploadEntities(graph: GraphShipment, neo4jSession: Session): Unit =
    for (group <- graph.entities.groupBy(entity => typeName(entity.proto))) {
      val (groupType, groupEntities) = group
      val entitiesMap = groupEntities.map(entity => new JMap[String, AnyRef](){
        put("id", entity.id)
        put("display", entity.proto.getFieldByNumber(1).toString)
      }).asJava
      val parameters: JMap[String, AnyRef] = new JMap[String, AnyRef](){
        put("entities", entitiesMap)
      }
      val statement =
        s"""
          WITH {entities} AS entities
          UNWIND entities AS entity
          MERGE (e:$groupType {architectId: entity.id, display: entity.display})
         """.stripMargin
      neo4jSession.run(statement, parameters)
    }

  /* Create indices in neo4j for each proto type on archerId */
  def createIndices(graph: GraphShipment, neo4jSession: Session): Unit =
    for (entityType <- graph.entities.map(entity => typeName(entity)).toSet[String])
      neo4jSession.run(
        s"CREATE INDEX ON :$entityType(architectId)"
      )

  override def flow(input: GraphShipment): GraphShipment = {
    val neo4jDriver = newNeo4jSession()
    val neo4jSession = neo4jDriver.session
    createIndices(input, neo4jSession); println("created indices")
    uploadEntities(input, neo4jSession); println("uploaded entities")
    uploadLinks(input, neo4jSession); println("uploaded links")
    neo4jDriver.close()
    neo4jSession.close()
    input
  }

}
