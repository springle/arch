package com.archerimpact.architect.arch.parsers.formats

import java.io.ByteArrayInputStream

import com.archerimpact.architect.arch.parsers.Parser
import com.archerimpact.architect.arch.shipments.{Entity, GraphShipment, Link}
import com.univocity.parsers.csv.{CsvParser, CsvParserSettings}

import scala.collection.JavaConverters._

abstract class CSVParser extends Parser {

  def mkEntity(params: String *): Option[Entity]
  def mkLink(entities: List[Entity], params: String *): Option[Link]

  override def parse(data: Array[Byte], url: String): GraphShipment = {

    /* ---------------- */
    /* Setup CSV Parser */
    /* ---------------- */

    val settings = new CsvParserSettings
    settings.getFormat.setLineSeparator("\n")
    val parser = new CsvParser(settings)
    val reader = new ByteArrayInputStream(data)

    /* ----------- */
    /* Build graph */
    /* ----------- */

    val entities = for {
      row <- parser.parseAll(reader).asScala
      entity <- mkEntity(row: _*)
    } yield entity

    val links = for {
      row <- parser.parseAll(reader).asScala
      link <- mkLink(entities.toList, row: _*)
    } yield link

    GraphShipment(entities.toList, links.toList, url)
  }

}
