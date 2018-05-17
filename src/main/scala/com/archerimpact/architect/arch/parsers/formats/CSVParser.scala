package com.archerimpact.architect.arch.parsers.formats

import java.io.ByteArrayInputStream

import com.archerimpact.architect.arch.parsers.Parser
import com.archerimpact.architect.arch.shipments.{Entity, GraphShipment, Link, PartialGraph}
import com.univocity.parsers.csv.{CsvParser, CsvParserSettings}

import scala.collection.JavaConverters._

abstract class CSVParser extends Parser {

  def source: String
  def mkGraph(url: String, params: String *): PartialGraph

  override def parse(data: Array[Byte], url: String): GraphShipment = {

    /* Setup CSV Parser */
    val settings = new CsvParserSettings
    settings.getFormat.setLineSeparator("\n")
    val parser = new CsvParser(settings)
    val reader = new ByteArrayInputStream(data)

    /* Build graph */
    val partialGraphs = for {
      row <- parser.parseAll(reader).asScala.tail
    } yield mkGraph(url, row: _*)
    PartialGraph.mergePartialGraphs(partialGraphs.toList, url, source)
  }

}
