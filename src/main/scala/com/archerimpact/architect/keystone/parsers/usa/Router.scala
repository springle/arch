package com.archerimpact.architect.keystone.parsers.usa

import akka.actor.{Actor, ActorRef, Props}
import com.archerimpact.architect.keystone._

object Router {
  def props(connector: ActorRef): Props = Props(new Router(connector))
}

class Router(connector: ActorRef) extends Actor {

  val myOfacParser: ActorRef = context.actorOf(OfacParser.props(connector), "ofac-parser")

  def routeShipment(shipment: Shipment): Unit = shipment match {
    case `shipment` if shipment.sourceName == "ofac" =>
      myOfacParser ! Parser.ParseShipment(shipment)
  }

  override def receive: Receive = {
    case Parser.ParseShipment(shipment) => routeShipment(shipment)
  }
}
