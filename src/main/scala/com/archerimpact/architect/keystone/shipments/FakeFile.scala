package com.archerimpact.architect.keystone.shipments

import com.archerimpact.architect.keystone.parsers.{DummyParser, Parser}

/* A shipment with fake data for testing */

class FakeFile(val url: String = "dum://my.source",
               val dataFormat: String = "dummy",
               val data: Array[Byte] = "dummyData".getBytes,
               val country: String = "dummy",
               val sourceName: String = "dummySource",
               val options: Map[String, String] = Map[String, String](),
               val parser: Parser = new DummyParser
              ) extends RawDataShipment

