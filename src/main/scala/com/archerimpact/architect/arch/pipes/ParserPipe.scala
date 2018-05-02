package com.archerimpact.architect.arch.pipes

import com.archerimpact.architect.arch.PipeSpec
import com.archerimpact.architect.arch.shipments.{FileShipment, GraphShipment}

class ParserPipe extends PipeSpec {
  override type InType = FileShipment
  override type OutType = GraphShipment
  override def flow(input: FileShipment): GraphShipment = input.parser.fileToGraph(input)
}
