package com.archerimpact.architect.keystone.pipes

import akka.actor.{ActorRef, Props}
import com.archerimpact.architect.keystone._
import com.archerimpact.architect.keystone.shipments.{GraphShipment, Shipment}
import com.sksamuel.elastic4s.bulk.BulkCompatibleDefinition
import com.sksamuel.elastic4s.http.ElasticDsl._

object ElasticPipe {
  def props(nextPipes: List[ActorRef]): Props = Props(new ElasticPipe(nextPipes))
}

class ElasticPipe(nextPipes: List[ActorRef]) extends PipeActor(nextPipes) {

  private val index = scala.util.Properties.envOrElse("ELASTIC_INDEX", "entities")
  private val elasticClient = newElasticClient()
  elasticClient.execute { createIndex (index) }

  def uploadEntities(graph: GraphShipment): Unit = {
    val commands: Seq[BulkCompatibleDefinition] = for (entity <- graph.entities) yield
      indexInto(s"$index/${protoType(entity.proto)}") id architectId(entity, graph) fields
        protoParams(entity.proto)
    elasticClient.execute { bulk(commands) }
  }

  override def processShipment(shipment: Shipment): GraphShipment = shipment match {
    case graph: GraphShipment => uploadEntities(graph); graph
  }

  override def updateStats(): Unit = context.parent ! KeystoneSupervisor.IncToElastic

}
