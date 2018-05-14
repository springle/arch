package com.archerimpact.architect.arch

import akka.actor.{ActorRef, Props}
import akka.http.scaladsl.model.{ContentTypes, HttpEntity}
import akka.http.scaladsl.server.{HttpApp, Route}
import com.archerimpact.architect.arch.pipes._
import com.archerimpact.architect.arch.shipments.UrlShipment
import com.sksamuel.elastic4s.http.{ElasticDsl, RequestFailure}
import com.sksamuel.elastic4s.http.ElasticDsl._
import org.neo4j.driver.v1.Value

import scala.collection.JavaConverters._
import org.json4s._
import org.json4s.JsonDSL._
import org.json4s.native.JsonMethods._
import org.json4s.JValue
import org.json4s.Extraction._

import scala.collection.mutable
import scala.collection.mutable.ListBuffer

object APISource extends HttpApp {

  implicit val formats: DefaultFormats.type = DefaultFormats

  override def routes: Route =
    parameters("id", "degrees") { (architect_id, degrees) =>

      //TODO: secure shit, validate architect id and degrees

      println(s"Getting $degrees degrees of data for node with id: $architect_id")

      degrees match {
        case "0" => {
          val singleNodeInfo: String = compact(render(decompose(getNodeInfo(architect_id.toString))))
          complete(HttpEntity(ContentTypes.`application/json`, "" + singleNodeInfo))
        } case _ => {
          val graphData = getFullGraph(architect_id, degrees)
          complete(HttpEntity(ContentTypes.`application/json`, "" + graphData))
        }
      }

    }


  def getFullGraph(architect_id: String, degrees: String): String = {
    //create new neo4j session
    val neo4jSession = newNeo4jSession()

    //query neo4j for all nodes connected to start node with architect_id
    var fullQuery =
      s"""MATCH path=(g)-[r*0..$degrees]-(p) WHERE g.architectId='$architect_id' UNWIND r as rel UNWIND nodes(path) as n RETURN COLLECT(distinct rel) AS collected, COLLECT(distinct n) as nodes, g""".stripMargin

    var resp = neo4jSession.run(fullQuery)
    //extract info from neo4j records response
    var hN = resp.hasNext

    var relationshipTuples = new ListBuffer[Map[String, String]]()
    var idMap = mutable.Map[String, String]()

    if (hN) {
      var record = resp.next()

      var rels = record.get("collected")
      var nodes = record.get("nodes")
      var thisNode = record.get("g")

      val relSize = rels.size()
      val numNodes = nodes.size()

      for (i <- 0 until numNodes) {
        var node = nodes.get(i).asNode()
        idMap.+=(node.id().toString -> node.get("architectId").toString)
      }

      for (i <- 0 until relSize) {
        var relation = rels.get(i).asRelationship()
        val start = idMap.get(relation.startNodeId().toString).get
        val end = idMap.get(relation.endNodeId().toString).get

        var cleanStart = start.toString.replace("\\","")
        cleanStart = cleanStart.substring(1, cleanStart.length-1)

        var cleanEnd = end.toString.replace("\\","")
        cleanEnd = cleanEnd.substring(1, cleanEnd.length-1)

        var relMap = mutable.Map[String, String]()
        relMap.+=("source" -> cleanStart.toString)
        relMap.+=("type" -> relation.`type`.toString)
        relMap.+=("target" -> cleanEnd.toString)

        relationshipTuples.+=(relMap.toMap)
      }

    }

    //implicit val formats: DefaultFormats.type = DefaultFormats

    var nodeMap = new ListBuffer[Map[String, AnyRef]]
    for (arch_id <- idMap.values.toList) {
      nodeMap.+=(getNodeInfo(arch_id.toString))
    }

    val relStr = compact(render(decompose(relationshipTuples)))
    val nodeStr = compact(render(decompose(nodeMap)))

    s"""{"nodes": $nodeStr, "relationships": $relStr}"""

  }

  def getNodeInfo(architect_id: String): Map[String, AnyRef] = {
    val elasticClient = newElasticClient()

    var cleaned_id = architect_id.replace("\\","")
    if (cleaned_id.charAt(0) == '"'){
      cleaned_id = cleaned_id.substring(1, cleaned_id.length-1)
    }

    val resp = elasticClient.execute{
      search("entities*") query idsQuery(cleaned_id)
    }.await

    resp match {
      case Left(failure) => mutable.Map[String, AnyRef]().toMap
      case Right(results) => {
        var matchHit = results.result.hits.hits(0)
        var retMap = matchHit.sourceAsMap
        retMap.+=("type" -> matchHit.`type`)
        retMap.+=("architectId" -> matchHit.id)
        retMap
      }
    }


  }

}
