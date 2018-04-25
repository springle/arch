package com.archerimpact.architect.keystone.sources

import akka.actor.{ActorRef, Props}
import com.archerimpact.architect.keystone.shipments.UrlShipment

object FakeNewsSource {
  def props(target: ActorRef): Props = Props(new FakeNewsSource(target))
}

class FakeNewsSource(target: ActorRef) extends SourceActor(target) {
  override def startSending(): Unit =
     for (_ <- 0 to 10)
       sendShipment(new UrlShipment("fake://fakeURL"))
}
