package com.archerimpact.architect.arch.parsers.countries.global

import com.archerimpact.architect.arch._
import com.archerimpact.architect.arch.shipments.{FileShipment, GraphShipment}
import org.scalatest._

class OpenSanctionsSpec extends FlatSpec with Matchers {

  val file: FileShipment = loadFromGoogle("gs://archer-source-data/global/opensanctions/un_sc_sanctions.csv")
  val graph: GraphShipment = file.parser.fileToGraph(file)

  "An opensanctions parser" should "be associated with files in .../global/opensanctions/" in {
    file.parser.isInstanceOf[parsers.countries.global.opensanctions] should be (true)
  }

}