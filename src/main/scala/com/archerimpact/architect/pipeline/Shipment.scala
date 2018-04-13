package com.archerimpact.architect.pipeline

abstract class Shipment {
  val data: Any
}

/* A dummy Shipment for testing */
class DummyShipment extends Shipment {
  val data: String = "Dummy shipment data"

  override def toString = "DummyShipment"
}

