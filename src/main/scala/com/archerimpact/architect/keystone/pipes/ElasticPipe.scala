package com.archerimpact.architect.keystone.pipes

import akka.actor.{ActorRef, Props}
import com.archerimpact.architect.keystone._
import com.archerimpact.architect.keystone.shipments.{GraphShipment, Shipment}
import com.sksamuel.elastic4s
import com.sksamuel.elastic4s.bulk.BulkCompatibleDefinition
import com.sksamuel.elastic4s.http.ElasticDsl._
import com.sksamuel.elastic4s.http.HttpClient

object ElasticPipe {
  def props(nextPipes: List[ActorRef]): Props = Props(new ElasticPipe(nextPipes))
}

class ElasticPipe(nextPipes: List[ActorRef]) extends PipeActor(nextPipes) {

  private val host = scala.util.Properties.envOrElse("ELASTIC_HOST", "localhost")
  private val port = scala.util.Properties.envOrElse("ELASTIC_PORT", "9200")
  private val index = scala.util.Properties.envOrElse("ELASTIC_INDEX", "entities")
  val client = HttpClient(elastic4s.ElasticsearchClientUri(host, port.toInt))
  client.execute { createIndex (index) }

  def uploadEntities(graph: GraphShipment): Unit = {
    val commands: Seq[BulkCompatibleDefinition] = for (entity <- graph.entities) yield
      indexInto(s"$index/${getProtoType(entity.proto)}") id architectId(entity, graph) fields
        getProtoParams(entity.proto)
    client.execute { bulk(commands) }
  }

  override def processShipment(shipment: Shipment): GraphShipment = shipment match {
    case graph: GraphShipment => uploadEntities(graph); graph
  }

  override def updateStats(): Unit = context.parent ! KeystoneSupervisor.IncToElastic

}
