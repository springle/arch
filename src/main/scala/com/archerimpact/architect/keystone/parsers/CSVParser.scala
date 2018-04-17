package com.archerimpact.architect.keystone.parsers

import akka.actor.ActorRef
import com.archerimpact.architect.keystone._

/* ---------------------- */
/* A Parser for CSV files */
/* ---------------------- */

abstract class CSVParser(val connector: ActorRef) extends Parser(connector) {

  type EntityType <: Entity

  val headers: List[String]

  override def parse(data: Any, url: String): Graph = {
    Graph(List(), List(), url)
  }

}
