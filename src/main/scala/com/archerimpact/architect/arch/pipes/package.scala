package com.archerimpact.architect.arch

import com.sksamuel.elastic4s.http.HttpClient
import com.sksamuel.elastic4s
import org.neo4j.driver.v1.{Driver, GraphDatabase, Session}

package object pipes {

  def newElasticClient(): HttpClient = {
    val host = scala.util.Properties.envOrElse("ELASTIC_HOST", "localhost")
    val port = scala.util.Properties.envOrElse("ELASTIC_PORT", "9200")
    HttpClient(elastic4s.ElasticsearchClientUri(host, port.toInt))
  }

  def newNeo4jSession(): Driver = {
    val host = scala.util.Properties.envOrElse("NEO4J_HOST", "localhost")
    val port = scala.util.Properties.envOrElse("NEO4J_PORT", "7687")
    val driver: Driver = GraphDatabase.driver(s"bolt://$host/$port")
    driver//.session
  }

}
