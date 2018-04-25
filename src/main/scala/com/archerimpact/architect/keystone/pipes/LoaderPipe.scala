package com.archerimpact.architect.keystone.pipes

import akka.actor.{ActorRef, Props}
import com.archerimpact.architect.keystone.KeystoneSupervisor
import com.archerimpact.architect.keystone.loaders.{FakeStorage, GoogleCloudStorage}
import com.archerimpact.architect.keystone.shipments.{UrlShipment, FileShipment, Shipment}

object LoaderPipe {
  def props(nextPipes: List[ActorRef]): Props = Props(new LoaderPipe(nextPipes))
}

class LoaderPipe(nextPipes: List[ActorRef]) extends PipeActor(nextPipes) {

  def load(fileURL: UrlShipment): FileShipment = fileURL match {
    case `fileURL` if fileURL.url.startsWith("gs://") => GoogleCloudStorage.urlToFile(fileURL)
    case `fileURL` if fileURL.url.startsWith("fake://") => FakeStorage.urlToFile(fileURL)
  }

  override def processShipment(shipment: Shipment): FileShipment = shipment match {
    case fileURL: UrlShipment => load(fileURL)
  }

  override def updateStats(): Unit = context.parent ! KeystoneSupervisor.IncLoaded
}
