package com.archerimpact.architect.keystone.pipes

import com.archerimpact.architect.keystone.PipeSpec
import com.archerimpact.architect.keystone.shipments.{FileShipment, GraphShipment}

class ParserPipe extends PipeSpec {
  override type InType = FileShipment
  override type OutType = GraphShipment
  override def flow(input: FileShipment): GraphShipment = input.parser.fileToGraph(input)
}
