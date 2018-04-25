package com.archerimpact.architect.keystone.parsers.formats

import java.io.ByteArrayInputStream

import com.archerimpact.architect.keystone.parsers.Parser
import com.archerimpact.architect.keystone.shipments.{Entity, GraphShipment}
import com.univocity.parsers.csv.{CsvParser, CsvParserSettings}

import scala.collection.JavaConverters._

abstract class CSV extends Parser {

  def mkEntity(params: String *): Option[Entity]

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

    GraphShipment(entities.toList, List(), url)
  }

}
