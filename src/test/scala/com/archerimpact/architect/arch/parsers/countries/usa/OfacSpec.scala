package com.archerimpact.architect.arch.parsers.countries.usa

import com.archerimpact.architect.arch._
import com.archerimpact.architect.arch.shipments.{FileShipment, GraphShipment}
import org.scalatest._

class OfacSpec extends FlatSpec with Matchers {

  val file: FileShipment = loadFromGoogle("gs://archer-source-data/usa/ofac/sdn.json")
  val graph: GraphShipment = file.parser.fileToGraph(file)

  "An ofac parser" should "be associated with files in .../usa/ofac/" in {
    file.parser.isInstanceOf[parsers.countries.usa.ofac] should be (true)
  }

  it should "build a graph with the expected number of entities" in {
    graph.entities.groupBy(entity => typeName(entity.proto)).
      map(group => group._1 match {
        case "organization"        => group._2.size should be (6522)
        case "vessel"              => group._2.size should be (120)
        case "aircraft"            => group._2.size should be (118)
        case "person"              => group._2.size should be (10270)
        case "identifyingDocument" => group._2.size should be (4521)
        case "address"             => group._2.size should be (6435)
        case _                     => None
      })
  }

  it should "build a graph whose first entity is AEROCARIBBEAN AIRLINES" in {
    graph.entities.head.id should be ("gs://archer-source-data/usa/ofac/sdn.json/36")
  }

}
