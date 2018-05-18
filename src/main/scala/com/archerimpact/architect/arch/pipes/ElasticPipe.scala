package com.archerimpact.architect.arch.pipes

import com.archerimpact.architect.arch._
import com.archerimpact.architect.arch.shipments.GraphShipment
import com.sksamuel.elastic4s.analyzers.KeywordAnalyzer
import com.sksamuel.elastic4s.bulk.BulkCompatibleDefinition
import com.sksamuel.elastic4s.http.ElasticDsl._
import scalapb.json4s.JsonFormat
import org.json4s.native.JsonMethods._
import org.json4s._


class ElasticPipe extends PipeSpec {

  override type InType = GraphShipment
  override type OutType = GraphShipment

  def uploadEntities(graph: GraphShipment): Unit = {
    val indexName = scala.util.Properties.envOrElse("ELASTIC_INDEX", "entities")
    val elasticClient = newElasticClient()
    elasticClient.execute {
      createIndex(indexName).mappings(
        mapping("person").fields(
          textField("name").analyzer(KeywordAnalyzer)
        ),
        mapping("organization").fields(
          textField("name").analyzer(KeywordAnalyzer)
        ),
        mapping("identifyingDocument").fields(
          textField("number").analyzer(KeywordAnalyzer)
        )
      )
    }.await
    val commands: Seq[BulkCompatibleDefinition] = for (entity <- graph.entities) yield {
       update(entity.id) in s"$indexName/${typeName(entity.proto)}" docAsUpsert compact(render(
         JsonFormat.toJson(entity.proto) merge JObject(List("dataset" -> JString(graph.source)))
       ))
    }

    elasticClient.execute { bulk(commands) }
  }

  override def flow(input: GraphShipment): GraphShipment = {
    uploadEntities(input); input
  }
}
