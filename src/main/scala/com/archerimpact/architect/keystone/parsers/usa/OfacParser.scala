package com.archerimpact.architect.keystone.parsers.usa

import akka.actor.{ActorRef, Props}
import com.archerimpact.architect.keystone.parsers.CSVParser

/* -------------------------------- */
/* Office of Foreign Assets Control */
/* -------------------------------- */

object OfacParser {
  def props(connector: ActorRef): Props = Props(new OfacParser(connector))
}

class OfacParser(connector: ActorRef) extends CSVParser(connector) {

  override val headers: List[String] = List(
    "entNum", "sdnName", "sdnType", "program", "title",
    "callSign", "vesselType", "tonnage", "grt", "vesselFlag",
    "vesselOwner", "remarks"
  )

}
