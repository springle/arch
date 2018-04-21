package com.archerimpact.architect.keystone.parsers.usa

import akka.actor.{ActorRef, Props}
import com.archerimpact.architect.keystone.parsers.CSVParserActor

/* -------------------------------- */
/* Office of Foreign Assets Control */
/* -------------------------------- */

object OfacParserActor {
  def props(connector: ActorRef): Props = Props(new OfacParserActor(connector))
}

class OfacParserActor(connector: ActorRef) extends CSVParserActor(connector) {

  override val headers: List[String] = List(
    "entNum", "sdnName", "sdnType", "program", "title",
    "callSign", "vesselType", "tonnage", "grt", "vesselFlag",
    "vesselOwner", "remarks"
  )

}
