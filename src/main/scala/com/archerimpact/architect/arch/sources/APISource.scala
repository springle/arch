package com.archerimpact.architect.arch

import akka.actor.{ActorRef, ActorSystem, Props}
import akka.http.scaladsl.model.headers.RawHeader
import akka.http.scaladsl.model.{ContentTypes, HttpEntity}
import akka.http.scaladsl.server.{Directive1, HttpApp, Route}
import akka.http.scaladsl._
import akka.http.scaladsl.unmarshalling.{FromRequestUnmarshaller, Unmarshal}
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
import akka.http.scaladsl.server.Directives
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import scalaj.http._

import scala.util.{Failure, Success, Try}
import com.sksamuel.elastic4s.http.search.SearchHit
//import spray.json._

import scala.concurrent.Future
import akka.http.scaladsl.model._
import akka.stream.ActorMaterializer

import org.json4s._
import org.json4s.native.JsonMethods.{parse=>parseJson}

import scala.collection.mutable
import scala.collection.mutable.ListBuffer

object APISource extends HttpApp {

  case class graphDataCarrier(nodes: ListBuffer[Map[String, AnyRef]], rels: ListBuffer[Map[String, String]])
  case class rawDataCarrier(nodes: List[String], rels: ListBuffer[Map[String, String]])

  implicit val formats: DefaultFormats.type = DefaultFormats

  //TODO: elasticsearch wrapper api

  override def routes: Route =

    parameters("search" ? "*", "id" ? "none", "degrees" ? "none", "expandby" ? "*", "exclude" ? "*", "attr" ? "*", "attrVal" ? "*", "from" ? "0", "size" ? "25") { (searchStr, architect_id, degrees, expand, exclude, attr, attrVal, from, size) =>

      //TODO: secure shit, validate architect id and degrees, no drop table, script tags, escape characters

      if (degrees != "none") println(s"Getting $degrees degrees of data for node with id: $architect_id")

      degrees match {
        case "0" => {
          val singleNodeInfo: String = getSingleNodeResponse(architect_id)

          respondWithHeader(RawHeader("Access-Control-Allow-Origin", "*")) {
            complete(HttpEntity(ContentTypes.`application/json`, "" + singleNodeInfo))
          }
        } case "1" => {
          if (!isValidID(architect_id)) {
            respondWithHeader(RawHeader("Access-Control-Allow-Origin", "*")) {
              complete(HttpEntity(ContentTypes.`application/json`, "Invalid ID. Please try again with a different id."))
            }
          } else {
            val expandJSON = getExpand(architect_id, expand, exclude, attr, attrVal)

            respondWithHeader(RawHeader("Access-Control-Allow-Origin", "*")) {
              complete(HttpEntity(ContentTypes.`application/json`, "" + expandJSON))
            }
          }

        } case "none" => {
          respondWithHeader(RawHeader("Access-Control-Allow-Origin", "*")) {
            complete(HttpEntity(ContentTypes.`application/json`, "" + searchWrapper(searchStr, from, size)))
          }
        } case _ => {
          respondWithHeader(RawHeader("Access-Control-Allow-Origin", "*")) {
            complete(HttpEntity(ContentTypes.`application/json`, "This endpoint has been deprecated."))
          }
        }
      }

    }

  def isValidID(id: String): Boolean = {
    Try(getNodeInfo(id)).isSuccess
  }

  def searchWrapper(queryStr: String, from:String = "0", size:String = "25"): String = {
    //var ESResponse = searchElasticSearch(queryStr)
    //"" + compact(render(decompose(ESResponse)))
    val fullQueryStr = queryStr + s"&from=$from&size=$size"
    println(s"Searching for $fullQueryStr")

    val seJSONStr = getSeJsonStr(fullQueryStr)

    val resultIDS = getSeResultIDs(seJSONStr)
    val resultScores = getResultScores(seJSONStr)
    val totalResults = getTotalResults(seJSONStr)

    val triedResults = resultIDS zip resultScores map {resultTuple =>
      Try(getNodeInfo(resultTuple._1) + ("exact" -> (resultTuple._2.toDouble > 200.0)))
    }
    val amendedResults = triedResults.collect{ case Success(r) => r}
    println("Total Responses:" + triedResults.size)
    println("Non-error responses:" + amendedResults.size)

    s"""{"num_results":$totalResults,"from":$from,"size":$size,"results":""" + compact(render(decompose(amendedResults))) + "}"
  }

//  def searchElasticSearch(queryStr: String): List[Map[String,AnyRef]] = {
//    val elasticClient = newElasticClient()
//
//    val resp = elasticClient.execute{
//      search("entities*") query queryStr limit 50
//    }.await
//
//    resp match {
//      case Left(failure) => {
//        elasticClient.close()
//        List()
//      }
//      case Right(results) => {
//        var matchHits = results.result.hits.hits
//        elasticClient.close()
//        sortResults(queryStr, matchHits.toList.map(parseSearchHit))
//      }
//    }
//  }
//
//  def sortResults(searchQuery: String, searchResults: List[Map[String, AnyRef]]): List[Map[String, AnyRef]] = {
//    var topList = mutable.ListBuffer[Map[String, AnyRef]]()
//    var bottomList = mutable.ListBuffer[Map[String, AnyRef]]()
//
//    var compareQueryStr = normalize(searchQuery)
//    //print(searchResults)
//    for (hit <- searchResults) {
//      var name = hit.getOrElse("name", "no name found on this entity").toString
//      if (tokenCompare(normalize(name), compareQueryStr)) {
//        topList.+=(hit + ("exact" -> "true"))
//      } else {
//        bottomList.+=(hit)
//      }
//    }
//    bottomList.++=:(topList).toList
//  }
//
//  def normalize(txt: String): String = {
//    txt.toLowerCase.replaceAll(",", "")
//  }
//
//  def tokenCompare(txt1: String, txt2: String): Boolean = {
//    var tokens1 = txt1.split(" ")
//    var tokens2 = txt2.split(" ")
//    for (tok1 <- tokens1) {
//      if (!tokens2.contains(tok1)) return false
//    }
//    true
//  }

  def getSeJsonStr(queryStr: String): String = {
    val seURL = ("https://sanctionsexplorer.org/search/sdn?all_fields=" + queryStr).replace(" ", "%20")
    println(s"Sending request to $seURL")
    val response: scalaj.http.HttpResponse[String] = scalaj.http.Http(seURL).asString
    response.body.toString
  }

  def getSeResultIDs(searchResponseStr: String): List[String] = {
    val seJSON = parseJson(searchResponseStr)
    seJSON.children.flatMap(getIDsFromChild)
  }

  def getResultScores(searchResponseStr: String): List[String] = {
    val seJSON = parseJson(searchResponseStr)
    (seJSON \ "response").children.map(x => x.children.last.extract[Double].toString)
  }

  def getIDsFromChild(child: JValue): List[String] = {
    (child \ "fixed_ref").extract[List[String]]
  }

  def getTotalResults(searchResponseStr: String): Int = {
    val seJSON = parseJson(searchResponseStr)
    (seJSON \ "num_results").extract[Int]
  }

  def parseSearchHit(hit: SearchHit): Map[String, AnyRef] = {
    var hitMap = hit.sourceAsMap
    hitMap.+=("type" -> hit.`type`)
    hitMap.+=("id" -> hit.id.toString)
    hitMap.+=("score" -> hit.score.toString)
    hitMap
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
        retMap.+=("type" -> formatType(matchHit.`type`))
        retMap.+=("id" -> matchHit.id)
        //println(getLinksCountForNode(matchHit.id))
        elasticClient.close()
        retMap
      }
    }

  }

  def formatType(tp: String): String = {
    tp match {
      case "identifyingDocument" => "Identifying Document"
      case "person" => "Person"
      case "organization" => "Organization"
      case "vessel" => "Vessel"
      case "aircraft" => "Aircraft"
      case _ => tp
    }
  }

  def getSingleNodeResponse(architect_id: String): String = {
    var lb = new ListBuffer[Map[String, AnyRef]]()
    lb.+=(getNodeInfo(architect_id))
    val jsonData: String = compact(render(decompose(lb)))
    s"""{"nodes" : $jsonData, "links": []}"""
  }

  def getLinksCountForNode(architect_id: String): Int = {
    val neo4jDriver = newNeo4jSession()
    val neo4jSession = neo4jDriver.session
    var fullQuery = s"""MATCH (n1)-[r]-() WHERE n1.architectId = '$architect_id' RETURN COUNT(r) as linksCount"""

    var resp = neo4jSession.run(fullQuery)

    //extract info from neo4j records response
    var hN = resp.hasNext
    if (!hN) {
      neo4jDriver.close()
      neo4jSession.close()
      return 0
    }

    var record = resp.next()
    var count = record.get("linksCount")

    neo4jDriver.close()
    neo4jSession.close()

    count.asInt()
  }

  def getRawNodesAndRelationships(architect_id: String, degrees: String): rawDataCarrier = {

    //query neo4j for all nodes connected to start node with architect_id
    val neo4jDriver = newNeo4jSession()
    val neo4jSession = neo4jDriver.session
    var fullQuery =
      s"""MATCH path=(g)-[r*0..$degrees]-(p) WHERE g.architectId='$architect_id' UNWIND r as rel UNWIND nodes(path) as n RETURN COLLECT(distinct rel) AS collected, COLLECT(distinct n) as nodes, g""".stripMargin

    var resp = neo4jSession.run(fullQuery)

    //extract info from neo4j records response
    var hN = resp.hasNext
    if (!hN) {
      //TODO: potentially add lonely nodes to list (if necessary)
      neo4jDriver.close()
      neo4jSession.close()
      return rawDataCarrier(List(), new ListBuffer[Map[String, String]]())
    }

    var relationshipTuples = new ListBuffer[Map[String, String]]()
    var idMap = mutable.Map[String, String]()

    var record = resp.next()

    var rels = record.get("collected")
    var nodes = record.get("nodes")
    var thisNode = record.get("g")

    neo4jDriver.close()
    neo4jSession.close()

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

    rawDataCarrier(idMap.values.toList, relationshipTuples)
  }

}
