package com.archerimpact.architect.arch.sources

import akka.actor.{ActorContext, ActorRef}
import com.archerimpact.architect.arch.SourceSpec
import com.archerimpact.architect.arch.shipments.UrlShipment

class FakeNewsSource(target: ActorRef) extends SourceSpec {
  override type OutType = UrlShipment
  override def run(send: OutType => Unit, context: ActorContext): Unit =
     for (_ <- 0 to 10)
       send(new UrlShipment("fake://fakeURL"))
}
