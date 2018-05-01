package com.archerimpact.architect.keystone.sources

import akka.actor.{ActorRef, Props}
import akka.http.scaladsl.model.{ContentTypes, HttpEntity}
import akka.http.scaladsl.server.{HttpApp, Route}
import com.archerimpact.architect.keystone.pipes._

import com.archerimpact.architect.keystone.shipments.{GraphShipment, UrlShipment}

object FrontendAPISource extends HttpApp {
  private val apiVersion = "v1"
  private val index = scala.util.Properties.envOrElse("ELASTIC_INDEX", "entities")
  private val elasticClient = newElasticClient()
  private val neo4jSession = newNeo4jSession()

  override def routes: Route =
    pathPrefix("graph") {
      path(Segment) { architect_id =>
        get {
          val ret = getGraphNode(architect_id)
          complete(HttpEntity(ContentTypes.`text/html(UTF-8)`, "" + ret))
        }
      }
    }

  def getGraphNode(architect_id: String): String = {
    var getData = s"MATCH (n1) WHERE n1.architect_id=$architect_id RETURN n1"
    //var fullQuery = s"MATCH path=(g)-[r*0..5]-(p) WHERE id(g)=$architect_id UNWIND r as rel UNWIND nodes(path) as n RETURN COLLECT(distinct rel) AS collected, COLLECT(distinct n) as nodes, g"
    var resp = neo4jSession.run(getData)
    resp.list().toString
  }

  def getNodeInfo(architect_id: String): String = {


  }

}
