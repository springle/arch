package com.archerimpact.architect.keystone.parsers

import com.archerimpact.architect.keystone.shipments.{Graph, RawFile}

trait Parser {
  def parse(data: Array[Byte], url: String): Graph
  def rawFileToGraph(rawFile: RawFile): Graph = parse(rawFile.data, rawFile.url)
}
