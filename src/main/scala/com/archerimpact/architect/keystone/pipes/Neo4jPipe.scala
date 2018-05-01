package com.archerimpact.architect.keystone.pipes

import java.util.{HashMap => JMap}

import com.archerimpact.architect.keystone._
import com.archerimpact.architect.keystone.shipments.GraphShipment
import org.neo4j.driver.v1.Session

import scala.collection.JavaConverters._

class Neo4jPipe extends PipeSpec {

  override type InType = GraphShipment
  override type OutType = GraphShipment

  def clean(s: Any): String = s.toString.replace("'", "").replace(".","")

  def uploadLinks(graph: GraphShipment, neo4jSession: Session): Unit =
    for (group <- graph.links.groupBy(link => link.predicate)) {
      val (groupPredicate, groupLinks) = group
      val links = groupLinks.map(link => new JMap[String, AnyRef]() {
        put("subjId", link.subjId)
        put("objId", link.objId)
      }).asJava
      val parameters: JMap[String, AnyRef] = new JMap[String, AnyRef]() {
        put("links", links)
      }
      val statement =
        s"""
            WITH {links} AS links
            UNWIND links AS link
            MATCH (subject), (object)
            WHERE subject.architectId = link.subjId
            AND object.architectId = link.objId
            MERGE (subject)-[:$groupPredicate]->(object)
         """.stripMargin
      neo4jSession.run(statement, parameters)
    }

  def uploadEntities(graph: GraphShipment, neo4jSession: Session): Unit =
    for (group <- graph.entities.groupBy(entity => typeName(entity.proto))) {
      val (groupType, groupEntities) = group
      val entitiesMap = groupEntities.map(entity => new JMap[String, AnyRef](){
        put("id", entity.id)
        put("display", clean(entity.proto.getFieldByNumber(1)))
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
