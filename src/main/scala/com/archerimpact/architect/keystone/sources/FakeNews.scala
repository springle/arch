package com.archerimpact.architect.keystone.sources

import akka.actor.{ActorRef, Props}
import com.archerimpact.architect.keystone.shipments.FileURL

object FakeNews {
  def props(target: ActorRef): Props = Props(new FakeNews(target))
}

class FakeNews(target: ActorRef) extends SourceActor(target) {
  override def startSending(): Unit =
     for (_ <- 0 to 10)
       sendShipment(new FileURL("fake://fakeURL"))
}
