package com.archerimpact.architect.arch.pipes

import com.archerimpact.architect.arch._
import com.archerimpact.architect.arch.shipments.GraphShipment
import com.sksamuel.elastic4s.bulk.BulkCompatibleDefinition
import com.sksamuel.elastic4s.http.ElasticDsl._
import scalapb.json4s.JsonFormat

class ElasticPipe extends PipeSpec {

  override type InType = GraphShipment
  override type OutType = GraphShipment

  private val index = scala.util.Properties.envOrElse("ELASTIC_INDEX", "entities")
  private val elasticClient = newElasticClient()
  elasticClient.execute { createIndex (index) }

  def uploadEntities(graph: GraphShipment): Unit = {
    val commands: Seq[BulkCompatibleDefinition] = for (entity <- graph.entities) yield
      indexInto(s"$index/${typeName(entity.proto)}") id entity.id doc JsonFormat.toJsonString(entity.proto)
    elasticClient.execute { bulk(commands) }
  }

  override def flow(input: GraphShipment): GraphShipment = {
    uploadEntities(input); input
  }
}
