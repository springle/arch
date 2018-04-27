package com.archerimpact.architect.keystone.sources

import akka.actor.{ActorRef, Props}
import com.archerimpact.architect.keystone.shipments.UrlShipment

object OCTestingSource {
  def props(target: ActorRef): Props = Props(new OCTestingSource(target))
}

class OCTestingSource(target: ActorRef) extends SourceActor(target) {
  override def startSending(): Unit =
    for (_ <- 0 to 10)
      sendShipment(new UrlShipment("gs://archer-source-data/global/opencorporates/json/gb-00000058.json"))
}
