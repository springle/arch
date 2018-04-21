package com.archerimpact.architect.keystone.parsers.uk

import akka.actor.{Actor, ActorRef, Props}
import com.archerimpact.architect.keystone._

object Router {
  def props(connector: ActorRef): Props = Props(new Router(connector))
}

class Router(connector: ActorRef) extends Actor {

  val myCompaniesHouseParser: ActorRef = context.actorOf(CompaniesHouseParserActor.props(connector), "companiesHouse-parser")

  def routeShipment(shipment: Shipment): Unit = shipment match {
    case `shipment` if shipment.sourceName == "companies_house" =>
      myCompaniesHouseParser ! ParserActor.ParseShipment(shipment)
  }

  override def receive: Receive = {
    case ParserActor.ParseShipment(shipment) => routeShipment(shipment)
    case KeystoneSupervisor.IncParsed => context.parent ! KeystoneSupervisor.IncParsed
  }
}
