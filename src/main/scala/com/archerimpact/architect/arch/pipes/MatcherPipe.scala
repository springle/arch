package com.archerimpact.architect.arch.pipes

import com.archerimpact.architect.arch._
import com.archerimpact.architect.arch.shipments.{GraphShipment, Link}
import com.sksamuel.elastic4s.http.ElasticDsl._
import com.sksamuel.elastic4s.http.search.SearchHit

class MatcherPipe extends PipeSpec {

  override type InType = GraphShipment
  override type OutType = GraphShipment

  private val index = scala.util.Properties.envOrElse("ELASTIC_INDEX", "entities")

  val matchable = Map("number" -> 0, "name" -> 0)

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
      (fieldName, fieldValue) <- protoParams(entity.proto) if matchable.contains(fieldName)
      response <- elasticClient.execute(
        search(s"$index/${typeName(entity.proto)}") query {
          constantScoreQuery {
            termQuery(fieldName, fieldValue.toString)
          }
        }
      ).await
      hit <- response.result.hits.hits
      if hit.id != entity.id
      if hit.sourceAsMap.getOrElse("dataset", "") != graph.source
    } uploadAndLogLink(entity.id, s"MATCHED_$fieldName".toUpperCase, hit.id)
  }

  override def flow(input: GraphShipment): GraphShipment = {
    matchGraph(input); input
  }

}
