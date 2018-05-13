package com.archerimpact.architect.arch.pipes

import com.archerimpact.architect.arch._
import com.archerimpact.architect.arch.shipments.{GraphShipment, Link}
import com.sksamuel.elastic4s.http.ElasticDsl._

class MatcherPipe extends PipeSpec {

  override type InType = GraphShipment
  override type OutType = GraphShipment

  private val index = scala.util.Properties.envOrElse("ELASTIC_INDEX", "entities")

  val matchable = Map("name" -> 1, "number" -> 0)

  def uploadAndLogLink(subj: String, pred: String, obj: String): Unit = {
    println(s"($subj)-[:$pred]-($obj)")
    uploadLink(Link(subj, pred, obj))
  }

  def uploadLink(link: Link): Unit = {
    val neo4jSession = newNeo4jSession()
    neo4jSession.run(
      s"""
          MATCH (subject)
          WHERE subject.architectId = '${link.subjId}'
          WITH subject
          MATCH (object)
          WHERE object.architectId = '${link.objId}'
          MERGE (subject)<-[:${link.predicate}]->(object)
       """.stripMargin
    )
  }

  def matchGraph(graph: GraphShipment): Unit = {
    val elasticClient = newElasticClient()
    for {
      entity <- graph.entities
      (fieldName, fieldValue) <- protoParams(entity.proto)
      if matchable.contains(fieldName)
      q = if (matchable(fieldName) > 0) fuzzyQuery(fieldName, fieldValue.toString)
      else termQuery(fieldName, fieldValue.toString)
      response <- elasticClient.execute(
        search(s"$index/${typeName(entity.proto)}") query q
      ).await
      hit <- response.result.hits.hits if hit.id != entity.id
    } uploadAndLogLink(entity.id, s"possibly_same_$fieldName".toUpperCase, hit.id)
  }

  override def flow(input: GraphShipment): GraphShipment = {
    matchGraph(input); input
  }

}
