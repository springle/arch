package com.archerimpact.architect.keystone.parsers

import akka.actor.ActorRef
import com.archerimpact.architect.keystone._

/* ---------------------- */
/* A Parser for JSON files */
/* ---------------------- */

abstract class JSONParserActor(val connector: ActorRef) extends ParserActor(connector) {

  override def parse(data: Any, url: String): Graph = {
    Graph(List(), List(), url)
  }

}
