package com.archerimpact.architect.pipeline

abstract class Shipment {
  val dataSource: String
  val data: Any
}

/* A dummy Shipment for testing */
class DummyShipment(
                     val dataSource: String = "DummySource",
                     val data: String = "DummyData"
                   ) extends Shipment

