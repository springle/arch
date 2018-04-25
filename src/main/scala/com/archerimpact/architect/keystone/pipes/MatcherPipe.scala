package com.archerimpact.architect.keystone.pipes

import akka.actor.{ActorRef, Props}
import com.archerimpact.architect.keystone.{KeystoneSupervisor, architectId, protoParams, protoType}
import com.archerimpact.architect.keystone.shipments.{GraphShipment, Shipment}
import com.sksamuel.elastic4s.http.ElasticDsl._

object MatcherPipe {
  def props(nextPipes: List[ActorRef]): Props = Props(new MatcherPipe(nextPipes))
}

class MatcherPipe(nextPipes: List[ActorRef]) extends PipeActor(nextPipes) {

  private val index = scala.util.Properties.envOrElse("ELASTIC_INDEX", "entities")
  private val elasticClient = newElasticClient()
  private val neo4jSession = newNeo4jSession()

  def matchGraph(graph: GraphShipment): Unit =
    for {
      entity <- graph.entities
      subjectId = architectId(entity, graph)
      (fieldName, fieldValue) <- protoParams(entity.proto)
      response <- elasticClient.execute(
        search(s"$index/${protoType(entity.proto)}") query termQuery(fieldName, fieldValue.toString)
      ).await
      result <- response
      hit <- result.hits.hits if hit.id != subjectId
    } println(s"$subjectId <-- same $fieldName --> ${hit.id}")

  override def processShipment(shipment: Shipment): GraphShipment = shipment match {
    case graph: GraphShipment => matchGraph(graph); graph
  }

  override def updateStats(): Unit = context.parent ! KeystoneSupervisor.IncMatched

}
