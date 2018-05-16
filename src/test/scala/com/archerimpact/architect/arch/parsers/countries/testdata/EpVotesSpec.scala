package com.archerimpact.architect.arch.parsers.countries.testdata

import com.archerimpact.architect.arch._
import com.archerimpact.architect.arch.shipments.{FileShipment, GraphShipment}
import org.scalatest.{FlatSpec, Matchers}

class EpVotesSpec extends FlatSpec with Matchers {

  val file: FileShipment = loadFromGoogle("gs://archer-source-data/testdata/ep_votes_trim/ep_votes_trim.json")
  val graph: GraphShipment = file.parser.fileToGraph(file)

  "An EpVotes parser" should "be associated with files in .../testdata/ep_votes_trim/" in {
    file.parser.isInstanceOf[parsers.countries.testdata.ep_votes_trim] should be (true)
  }

  it should "render a graph" in {
    println(graph)
    println(graph.entities.size)
    println(graph.links.size)
  }

}
