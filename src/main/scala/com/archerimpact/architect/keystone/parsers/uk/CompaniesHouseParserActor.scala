package com.archerimpact.architect.keystone.parsers.uk

import akka.actor.{ActorRef, Props}
import com.archerimpact.architect.keystone.parsers.JSONParserActor

/* --------------- */
/* Companies House */
/* --------------- */

object CompaniesHouseParserActor {
  def props(connector: ActorRef): Props = Props(new CompaniesHouseParserActor(connector))
}

class CompaniesHouseParserActor(connector: ActorRef) extends JSONParserActor(connector)
