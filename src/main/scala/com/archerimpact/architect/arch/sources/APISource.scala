package com.archerimpact.architect.arch

import akka.actor.{ActorRef, Props}
import akka.http.scaladsl.model.headers.RawHeader
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
import org.neo4j.driver.v1.types.Relationship

import scala.collection.mutable
import scala.collection.mutable.ListBuffer

object APISource extends HttpApp {

  implicit val formats: DefaultFormats.type = DefaultFormats

  override def routes: Route =
    parameters("id", "degrees", "expand") { (architect_id, degrees, expand) =>

      //TODO: secure shit, validate architect id and degrees

      println(s"Getting $degrees degrees of data for node with id: $architect_id")

      degrees match {
        case "0" => {
          var lb = new ListBuffer[Map[String, AnyRef]]()
          lb.+=(getNodeInfo(architect_id.toString))
          val jsonData: String = compact(render(decompose(lb)))
          var singleNodeInfo: String = s"""{"nodes" : $jsonData, "links": []}"""

          respondWithHeader(RawHeader("Access-Control-Allow-Origin", "*")) {
            complete(HttpEntity(ContentTypes.`application/json`, "" + singleNodeInfo))
          }
        } case "1" => {
          val graphJSON = getExpand(architect_id, expand)
          respondWithHeader(RawHeader("Access-Control-Allow-Origin", "*")) {
            complete(HttpEntity(ContentTypes.`application/json`, "" + graphJSON))
          }
        } case _ => {
          val graphData = getFullGraph(architect_id, degrees)
          respondWithHeader(RawHeader("Access-Control-Allow-Origin", "*")) {
            complete(HttpEntity(ContentTypes.`application/json`, "" + graphData))
          }
        }
      }

    }


  def getExpand(architect_id: String, expand: String): String = {
    val degrees = 1

    val neo4jSession = newNeo4jSession()

    //query neo4j for all nodes connected to start node with architect_id
    var fullQuery =
      s"""MATCH path=(g)-[r*0..1]-(p) WHERE g.architectId='$architect_id' UNWIND r as rel UNWIND nodes(path) as n RETURN COLLECT(distinct rel) AS collected, COLLECT(distinct n) as nodes, g""".stripMargin

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
        relMap.+=("id" -> ("" + cleanStart.toString + relation.`type`.toString + cleanEnd.toString))

        relationshipTuples.+=(relMap.toMap)


      }

    }

    var linksMap = getNeighborLinkCounts(architect_id, degrees.toInt)

    var nodeMap = new ListBuffer[Map[String, AnyRef]]
    for (arch_id <- idMap.values.toList) {
      var nd = mutable.Map() ++ getNodeInfo(arch_id.toString)
      var linksCountMap = mutable.Map() ++ linksMap.get(nd.get("id").get.toString).get
      var total = 0
      var microLinksMap = mutable.Map[String, String]()
      for (tp <- linksCountMap.keys) {
        var count = linksCountMap.get(tp).get
        microLinksMap.+=(tp.toString -> count.toString)
        total += count
      }
      nd.+=("linkTypes" -> microLinksMap)
      nd.+=("totalLinks" -> total.toString)
      nodeMap.+=(nd.toMap)
    }

    expand match {
      case "*" => {
        val relStr = compact(render(decompose(relationshipTuples)))
        val nodeStr = compact(render(decompose(nodeMap)))

        s"""{"nodes": $nodeStr, "links": $relStr}"""
      } case _ => {
        var newNodes = new ListBuffer[Map[String, AnyRef]]
        var newRels = new ListBuffer[Map[String, String]]

        var whiteList = mutable.SortedSet[String]()

        for (rel <- relationshipTuples) {
          if (rel.get("type").get == expand) {
            whiteList += rel.get("target").get
            whiteList += rel.get("source").get
            newRels.+=(rel)
          }
        }

        for (node <- nodeMap) {
          if (whiteList.contains(node.get("id").get.toString)) {
            newNodes.+=(node)
          }
        }

        val relStr = compact(render(decompose(newRels)))
        val nodeStr = compact(render(decompose(newNodes)))

        s"""{"nodes": $nodeStr, "links": $relStr}"""
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
        relMap.+=("id" -> ("" + cleanStart.toString + relation.`type`.toString + cleanEnd.toString))

        relationshipTuples.+=(relMap.toMap)


      }

    }

    var linksMap = getNeighborLinkCounts(architect_id, degrees.toInt)

    var nodeMap = new ListBuffer[Map[String, AnyRef]]
    for (arch_id <- idMap.values.toList) {
      var nd = mutable.Map() ++ getNodeInfo(arch_id.toString)
      var linksCountMap = mutable.Map() ++ linksMap.get(nd.get("id").get.toString).get
      var total = 0
      var microLinksMap = mutable.Map[String, String]()
      for (tp <- linksCountMap.keys) {
        var count = linksCountMap.get(tp).get
        microLinksMap.+=(tp.toString -> count.toString)
        total += count
      }
      nd.+=("linkTypes" -> microLinksMap)
      nd.+=("totalLinks" -> total.toString)
      nodeMap.+=(nd.toMap)
    }

    val relStr = compact(render(decompose(relationshipTuples)))
    val nodeStr = compact(render(decompose(nodeMap)))

    s"""{"nodes": $nodeStr, "links": $relStr}"""

  }

  def getNeighborLinkCounts(architect_id: String, degrees: Int): Map[String, Map[String, Int]] = {
    //gets all relationships in a list of all nodes within x degrees
    val relList = getAllRelationships(architect_id, (degrees+1).toString)

    var idToCountMap = mutable.Map[String, Map[String, Int]]()
    for (relMap <- relList) {
      var start: String = relMap.get("source").get
      var end: String = relMap.get("target").get
      var tp: String = relMap.get("type").get

      var adder = 1

      for (rel_id <- List(start, end)) {
        if (idToCountMap.contains(rel_id)) {
          var linkMap = mutable.Map() ++ idToCountMap.get(rel_id).get
          if (linkMap.contains(tp)) {
            linkMap.update(tp, linkMap.get(tp).get + adder)
          } else {
            linkMap.+=(tp -> adder)
          }
          idToCountMap.update(rel_id, linkMap.toMap)
        } else {
          var linkMap = mutable.Map[String, Int]()
          linkMap.+=(tp -> adder)
          idToCountMap.+=(rel_id -> linkMap.toMap)
        }
      }

//      if (idToCountMap.contains(start)) {
//        var linkMap = idToCountMap.get(start).get
//        if (linkMap.contains(tp)) linkMap.update(tp, linkMap.get(tp).get + adder)
//        else linkMap.+=(tp -> adder)
//        idToCountMap.update(start, linkMap)
//      } else {
//        idToCountMap.+=(start -> adder)
//      }
//
//      if (idToCountMap.contains(end)) {
//        idToCountMap.update(end, idToCountMap.get(end).get + adder)
//      } else {
//        idToCountMap.+=(end -> adder)
//      }
    }

    idToCountMap.toMap

  }

  def getAllRelationships(architect_id: String, degrees: String): ListBuffer[Map[String, String]] = {
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

    relationshipTuples
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
        retMap.+=("id" -> matchHit.id)
        retMap
      }
    }


  }

}
