package com.archerimpact.architect.keystone

import akka.actor.{ActorRef, Props}
import com.archerimpact.architect.keystone.loaders.{FakeStorage, GoogleCloudStorage}
import com.archerimpact.architect.keystone.shipments.{FileURL, RawFile, Shipment}

object LoaderPipe {
  def props(nextPipes: List[ActorRef]): Props = Props(new LoaderPipe(nextPipes))
}

class LoaderPipe(nextPipes: List[ActorRef]) extends PipeActor(nextPipes) {

  def load(fileURL: FileURL): RawFile = fileURL match {
    case `fileURL` if fileURL.url.startsWith("gs://") => GoogleCloudStorage.fileURLToRawFile(fileURL)
    case `fileURL` if fileURL.url.startsWith("fake://") => FakeStorage.fileURLToRawFile(fileURL)
  }

  override def processShipment(shipment: Shipment): Shipment = shipment match {
    case fileURL: FileURL => load(fileURL)
  }

  override def updateStats(): Unit = context.parent ! KeystoneSupervisor.IncLoaded
}
