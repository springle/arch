package com.archerimpact.architect.keystone.pipes

import com.archerimpact.architect.keystone._
import com.archerimpact.architect.keystone.shipments.{GraphShipment, Shipment}
import com.sksamuel.elastic4s.http.ElasticDsl._

class MatcherPipe extends PipeSpec {

  override type InType = GraphShipment
  override type OutType = GraphShipment

  private val index = scala.util.Properties.envOrElse("ELASTIC_INDEX", "entities")
  private val elasticClient = newElasticClient()
  // private val neo4jSession = newNeo4jSession()

  def matchGraph(graph: GraphShipment): Unit =
    for {
      entity <- graph.entities
      subjectId = architectId(entity, graph)
      (fieldName, fieldValue) <- protoParams(entity.proto)
      response <- elasticClient.execute(
        search(s"$index/${typeName(entity.proto)}") query termQuery(fieldName, fieldValue.toString)
      ).await
      result <- response
      hit <- result.hits.hits if hit.id != subjectId
    } println(s"$subjectId <-- same $fieldName --> ${hit.id}")

  override def flow(input: GraphShipment): GraphShipment = {
    matchGraph(input); input
  }

}
