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

  case class graphDataCarrier(nodes: ListBuffer[Map[String, AnyRef]], rels: ListBuffer[Map[String, String]])
  case class rawDataCarrier(nodes: List[String], rels: ListBuffer[Map[String, String]])

  implicit val formats: DefaultFormats.type = DefaultFormats

  override def routes: Route =
    parameters("id", "degrees", "expandby", "exclude", "attr", "attrVal") { (architect_id, degrees, expand, exclude, attr, attrVal) =>

      //TODO: secure shit, validate architect id and degrees, no drop table, script tags, escape characters

      println(s"Getting $degrees degrees of data for node with id: $architect_id")

      degrees match {
        case "0" => {
          val singleNodeInfo: String = getSingleNodeResponse(architect_id)

          respondWithHeader(RawHeader("Access-Control-Allow-Origin", "*")) {
            complete(HttpEntity(ContentTypes.`application/json`, "" + singleNodeInfo))
          }
        } case "1" => {
          val expandJSON = getExpand(architect_id, expand, exclude, attr, attrVal)

          respondWithHeader(RawHeader("Access-Control-Allow-Origin", "*")) {
            complete(HttpEntity(ContentTypes.`application/json`, "" + expandJSON))
          }
        } case _ => {
          respondWithHeader(RawHeader("Access-Control-Allow-Origin", "*")) {
            complete(HttpEntity(ContentTypes.`application/json`, "The endpoint for degrees > 1 has been deprecated."))
          }
        }
      }

    }

  def filterAttribute(data: graphDataCarrier, attrName: String, attrValue: String): graphDataCarrier = {
    var nodes = data.nodes
    var rels = data.rels

    var newNodes = new ListBuffer[Map[String, AnyRef]]
    var newRels = new ListBuffer[Map[String, String]]

    var whiteList = mutable.SortedSet[String]()

    for (nd <- nodes) {
      var attr = "NOT_FOUND"
      if (nd.contains(attrName)) {
        attr = nd.get(attrName).get.toString
      }

      if (attr == attrValue || (attrName=="totalLinks" && attr.toInt >= attrValue.toInt)) {
        whiteList.+=(nd.get("id").get.toString)
        newNodes.+=(nd)
      }
    }

    for (rel <- rels) {
      var source = rel.get("source").get
      var target = rel.get("target").get

      if (whiteList.contains(source) && whiteList.contains(target)) newRels.+=(rel)
    }

    graphDataCarrier(newNodes, newRels)

  }

  def filterUpGraph(data: graphDataCarrier, filterString: String): graphDataCarrier = {
    var nodes = data.nodes
    var rels = data.rels

    var newNodes = new ListBuffer[Map[String, AnyRef]]
    var newRels = new ListBuffer[Map[String, String]]

    var whiteList = mutable.SortedSet[String]()

    for (rel <- rels) {

      var tp: String = rel.get("type").get

      var decider = true
      if (filterString.contains(",")){
        var filters = filterString.split(",")
        decider = filters.contains(tp)
      } else {
        decider = tp == filterString
      }

      if (decider) {
        whiteList += rel.get("target").get
        whiteList += rel.get("source").get
        newRels.+=(rel)
      }
    }

    for (node <- nodes) {
      if (whiteList.contains(node.get("id").get.toString)) {
        newNodes.+=(node)
      }
    }

    graphDataCarrier(newNodes, newRels)
  }

  def filterGraph(data: graphDataCarrier, filterString: String, exclude: Boolean): graphDataCarrier = {
    var nodes = data.nodes
    var rels = data.rels

    var newNodes = new ListBuffer[Map[String, AnyRef]]
    var newRels = new ListBuffer[Map[String, String]]

    var whiteList = mutable.SortedSet[String]()

    for (rel <- rels) {

      var decider = true
      if (exclude == true) {
        decider = rel.get("type").get != filterString
      } else {
        decider = rel.get("type").get == filterString
      }

      if (decider) {
        whiteList += rel.get("target").get
        whiteList += rel.get("source").get
        newRels.+=(rel)
      }
    }

    for (node <- nodes) {
      if (whiteList.contains(node.get("id").get.toString)) {
        newNodes.+=(node)
      }
    }

    graphDataCarrier(newNodes, newRels)
  }

  def getExpand(architect_id: String, expand: String, exclude: String, attr: String, attrVal: String): String = {
    val degrees = 1

    var oldData = getGraphData(architect_id, degrees.toString)

    var newData = oldData
    var ex = true
    var filterString = exclude
    if (expand != "*") {
      ex = false
      filterString = expand
      newData = filterUpGraph(newData, filterString)
    } else if (exclude != "*") {
      if (filterString.contains(",")) {
        var filterList = filterString.split(",")
        for (flt <- filterList) {
          newData = filterGraph(newData, flt, ex)
        }
      } else {
        newData = filterGraph(oldData, filterString, ex)
      }
    }
    if (attr != "*") {
      newData = filterAttribute(newData, attr, attrVal)
    }

    var endRels = newData.rels
    var endNodes = newData.nodes

    //TODO: not sure if this is the right metric for this. totalCounts = 0
    if (endNodes.size == 0) {
      var lb = new ListBuffer[Map[String, AnyRef]]()
      var lonelyNode = mutable.Map() ++ getNodeInfo(architect_id.toString)
      lonelyNode.+=("totalLinks" -> "0", "linkTypes" -> mutable.Map[String, String]())
      lb.+=(lonelyNode.toMap)
      endNodes = lb
    }

    val relStr = compact(render(decompose(endRels)))
    val nodeStr = compact(render(decompose(endNodes)))
    s"""{"nodes": $nodeStr, "links": $relStr}"""

  }

  def getGraphData(architect_id: String, degrees: String): graphDataCarrier = {
    val rawData = getRawNodesAndRelationships(architect_id, degrees)
    var relationshipTuples = rawData.rels

    var linksMap = getNeighborLinkCounts(architect_id, degrees.toInt)

    var nodeMap = new ListBuffer[Map[String, AnyRef]]
    for (arch_id <- rawData.nodes) {
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

    graphDataCarrier(nodeMap, relationshipTuples)

  }

  def getNeighborLinkCounts(architect_id: String, degrees: Int): Map[String, Map[String, Int]] = {
    //gets all relationships in a list of all nodes within x degrees
    //val relList = getAllRelationships(architect_id, (degrees+1).toString)
    val relList = getRawNodesAndRelationships(architect_id, (degrees+1).toString).rels

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

    }

    idToCountMap.toMap

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
      case Left(failure) => {
        elasticClient.close()
        mutable.Map[String, AnyRef]().toMap
      }
      case Right(results) => {
        var matchHit = results.result.hits.hits(0)
        var retMap = matchHit.sourceAsMap
        retMap.+=("type" -> matchHit.`type`)
        retMap.+=("id" -> matchHit.id)
        elasticClient.close()
        retMap
      }
    }


  }

  def getSingleNodeResponse(architect_id: String): String = {
    var lb = new ListBuffer[Map[String, AnyRef]]()
    lb.+=(getNodeInfo(architect_id))
    val jsonData: String = compact(render(decompose(lb)))
    s"""{"nodes" : $jsonData, "links": []}"""
  }

  def getRawNodesAndRelationships(architect_id: String, degrees: String): rawDataCarrier = {
    val neo4jSession = newNeo4jSession()

    //query neo4j for all nodes connected to start node with architect_id
    var fullQuery =
      s"""MATCH path=(g)-[r*0..$degrees]-(p) WHERE g.architectId='$architect_id' UNWIND r as rel UNWIND nodes(path) as n RETURN COLLECT(distinct rel) AS collected, COLLECT(distinct n) as nodes, g""".stripMargin

    var resp = neo4jSession.run(fullQuery)
    //extract info from neo4j records response
    var hN = resp.hasNext
    if (!hN) {
      //TODO: potentially add lonely nodes to list (if necessary)
      neo4jSession.close()
      return rawDataCarrier(List(), new ListBuffer[Map[String, String]]())
    }

    var relationshipTuples = new ListBuffer[Map[String, String]]()
    var idMap = mutable.Map[String, String]()

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

    neo4jSession.close()
    rawDataCarrier(idMap.values.toList, relationshipTuples)
  }

}
