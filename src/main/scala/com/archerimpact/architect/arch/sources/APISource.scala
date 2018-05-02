package com.archerimpact.architect.arch

import akka.actor.{ActorRef, Props}
import akka.http.scaladsl.model.{ContentTypes, HttpEntity}
import akka.http.scaladsl.server.{HttpApp, Route}
import com.archerimpact.architect.arch.pipes._

import com.archerimpact.architect.arch.shipments.UrlShipment

object APISource extends HttpApp {
  private val apiVersion = "v1"
  private val index = scala.util.Properties.envOrElse("ELASTIC_INDEX", "entities")
  private val elasticClient = newElasticClient()

  override def routes: Route =
    pathPrefix("graph") {
      path(Segment) { architect_id =>
        get {
          val node = getGraphNode(architect_id)
          complete(HttpEntity(ContentTypes.`text/html(UTF-8)`, "" + node))
        }
      }
    }

  def getGraphNode(architect_id: String): String = {
    val neo4jSession = newNeo4jSession()
    //var getData = s"MATCH (n1) WHERE n1.architect_id=$architect_id RETURN n1"
    //TODO: change id to architect_id
    var fullQuery =
    s"""
        MATCH path=(g)-[r*0..5]-(p) WHERE id(g)=$architect_id UNWIND r as rel UNWIND nodes(path) as n
        RETURN COLLECT(distinct rel) AS collected, COLLECT(distinct n) as nodes, g
      """.stripMargin
    var resp = neo4jSession.run(fullQuery)
    resp.list().toString()
  }

}
