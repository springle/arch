package com.archerimpact.architect.arch

import akka.actor.{ActorRef, Props}
import akka.http.scaladsl.model.{ContentTypes, HttpEntity}
import akka.http.scaladsl.server.{HttpApp, Route}
import com.archerimpact.architect.arch.pipes._
import com.archerimpact.architect.arch.shipments.UrlShipment
import com.sksamuel.elastic4s.http.ElasticDsl.{search, termQuery}
import org.neo4j.driver.v1.Value

import scala.collection.JavaConverters._
import org.json4s._
import org.json4s.JsonDSL._
import org.json4s.native.JsonMethods._

import scala.collection.mutable
import scala.collection.mutable.ListBuffer

object APISource extends HttpApp {
  private val apiVersion = "v1"
  private val index = scala.util.Properties.envOrElse("ELASTIC_INDEX", "entities")


  override def routes: Route =
    pathPrefix("graph") {
      path(Segment) { architect_id =>
        get {
          val node = getFullGraph(architect_id)
          complete(HttpEntity(ContentTypes.`application/json`, "" + node))
        }
      }
    }

  def getGraphNode(architect_id: String): String = {
    val neo4jSession = newNeo4jSession()

    //TODO: change id to architect_id
    val fullQuery =
      s"""
         MATCH (n1) WHERE n1.architect_id=$architect_id RETURN n1
      """.stripMargin

    val resp = neo4jSession.run(fullQuery)
    resp.list().toString
  }

  def getFullGraph(architect_id: String): String = {
    //create new neo4j session
    val neo4jSession = newNeo4jSession()

    //query neo4j for all nodes connected to start node with architect_id
    //TODO: change id to architect id
    var fullQuery =
      s"""MATCH path=(g)-[r*0..5]-(p) WHERE id(g)=$architect_id UNWIND r as rel UNWIND nodes(path) as n RETURN COLLECT(distinct rel) AS collected, COLLECT(distinct n) as nodes, g""".stripMargin

    var resp = neo4jSession.run(fullQuery)

    //extract info from neo4j records response
    var hN = resp.hasNext
    var relationshipTuples = new ListBuffer[List[String]]()

    var idMap = mutable.Map[String, String]()

    if (hN) {
      var record = resp.next()

      var rels = record.get("collected")
      var nodes = record.get("nodes")
      var thisNode = record.get("g")

      val relSize = rels.size()
      val numNodes = nodes.size()

      for (i <- 0 to numNodes-1) {
        var node = nodes.get(i).asNode()
        idMap.+=(node.id().toString -> node.get("architect_id").toString)
      }

      for (i <- 0 to relSize-1) {
        var relation = rels.get(i).asRelationship()
        relationshipTuples.+=(List(relation.startNodeId().toString, relation.`type`(), relation.endNodeId().toString))
      }


    }

    //compact(render(relationshipTuples))
    compact(render(idMap))

  }

  def getNodeInfo(architect_id: String): String = {
    val elasticClient = newElasticClient()

    //val resp = elasticClient.execute(search(s"$index/${typeName(entity.proto)}") query termQuery(fieldName, fieldValue.toString))
    return ""
  }

}
