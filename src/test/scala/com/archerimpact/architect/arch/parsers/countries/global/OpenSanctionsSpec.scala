package com.archerimpact.architect.arch.parsers.countries.global

import com.archerimpact.architect.arch._
import com.archerimpact.architect.arch.shipments.{FileShipment, GraphShipment}
import org.scalatest._

class OpenSanctionsSpec extends FlatSpec with Matchers {

  val file1: FileShipment = loadFromGoogle("gs://archer-source-data/global/opensanctions/un_sc_sanctions.csv")
  val file2: FileShipment = loadFromGoogle("gs://archer-source-data/global/opensanctions/un_sc_sanctions_addresses.csv")
  val file3: FileShipment = loadFromGoogle("gs://archer-source-data/global/opensanctions/un_sc_sanctions_aliases.csv")
  val file4: FileShipment = loadFromGoogle("gs://archer-source-data/global/opensanctions/un_sc_sanctions_birth_dates.csv")
  val file5: FileShipment = loadFromGoogle("gs://archer-source-data/global/opensanctions/un_sc_sanctions_identifiers.csv")
  val file6: FileShipment = loadFromGoogle("gs://archer-source-data/global/opensanctions/un_sc_sanctions_nationalities.csv")
  val graph1: GraphShipment = file1.parser.fileToGraph(file1)
  val graph2: GraphShipment = file2.parser.fileToGraph(file2)
  val graph3: GraphShipment = file3.parser.fileToGraph(file3)
  val graph4: GraphShipment = file4.parser.fileToGraph(file4)
  val graph5: GraphShipment = file5.parser.fileToGraph(file5)
  val graph6: GraphShipment = file6.parser.fileToGraph(file6)

  "An opensanctions parser" should "be associated with files in .../global/opensanctions/" in {
    file1.parser.isInstanceOf[parsers.countries.global.opensanctions] should be (true)
    file2.parser.isInstanceOf[parsers.countries.global.opensanctions] should be (true)
    file3.parser.isInstanceOf[parsers.countries.global.opensanctions] should be (true)
    file4.parser.isInstanceOf[parsers.countries.global.opensanctions] should be (true)
    file5.parser.isInstanceOf[parsers.countries.global.opensanctions] should be (true)
    file6.parser.isInstanceOf[parsers.countries.global.opensanctions] should be (true)
  }

}