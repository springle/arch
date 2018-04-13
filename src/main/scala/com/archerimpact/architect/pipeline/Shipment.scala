package com.archerimpact.architect.pipeline

abstract class Shipment {
  val dataSource: String
  val data: Any
}

/* A dummy Shipment for testing */
class DummyShipment extends Shipment {
  val dataSource = "DummySource"
  val data = "DummyData"
}

