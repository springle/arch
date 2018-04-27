package com.archerimpact.architect.keystone.sources

import akka.actor.{ActorContext, ActorRef}
import com.archerimpact.architect.keystone.SourceSpec
import com.archerimpact.architect.keystone.shipments.UrlShipment

class FakeNewsSource(target: ActorRef) extends SourceSpec {
  override type OutType = UrlShipment
  override def run(send: OutType => Unit, context: ActorContext): Unit =
     for (_ <- 0 to 10)
       send(new UrlShipment("fake://fakeURL"))
}
