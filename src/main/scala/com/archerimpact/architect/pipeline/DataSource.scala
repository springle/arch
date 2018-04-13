package com.archerimpact.architect.pipeline

import akka.actor.ActorRef

trait DataSource {
  def startSending(target: ActorRef)
}

/* A dummy DataSource for testing */

class DummyDataSource extends DataSource {
  override def startSending(target: ActorRef): Unit = {
    for (_ <- 0 to 10)
      target ! LoaderSupervisor.LoadShipment(new DummyShipment)
  }
  override def toString = "DummyDataSource"
}