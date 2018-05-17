package com.archerimpact.architect.arch.parsers.countries.global

import com.archerimpact.architect.arch.parsers.formats.CSVParser
import com.archerimpact.architect.arch.shipments.{Entity, Link, PartialGraph}
import com.archerimpact.architect.ontology._

class opensanctions extends CSVParser {
  val source = "Open Sanctions"

  def addressesGraph(params: String*): PartialGraph = PartialGraph()
  def aliasesGraph(params: String*): PartialGraph = PartialGraph()
  def birthDatesGraph(params: String*): PartialGraph = PartialGraph()
  def nationalitiesGraph(params: String*): PartialGraph = PartialGraph()

  def identifiersGraph(params: String*): PartialGraph = params match {
    case Seq(
      id, description, lastSeen,
      number, countryName, countryCode,
      issuedAt, entityType, firstSeen
    ) =>
      val proto = identifyingDocument(number, entityType, countryName, countryName)
      val idDoc = Entity(s"$id/idDoc/$number", proto)
      val entities = List(idDoc)
      val link = Link(id, "HAS_ID_DOC", s"$id/idDoc/$number")
      val links = List(link)
      PartialGraph(entities, links)
  }

  def sanctionsGraph(params: String*): PartialGraph = params match {
    case Seq(
      lastName, lastSeen, thirdName,
      updatedAt, fatherName, id,
      firstName, title, dataSource,
      program, entityType, function,
      listedAt, name, url,
      gender, summary, secondName,
      firstSeen
      ) =>
        val proto = entityType match {
          case "individual" => person(name)
          case "entity" => organization(name)
        }
        val entity = Entity(id, proto)
        val entities = List(entity)
        val links = List[Link]()
        PartialGraph(entities, links)
  }

  override def mkGraph(url: String, params: String*): PartialGraph = url match {
    case _ if url.endsWith("un_sc_sanctions_addresses.csv") => addressesGraph(params: _*)
    case _ if url.endsWith("un_sc_sanctions_aliases.csv") => aliasesGraph(params: _*)
    case _ if url.endsWith("un_sc_sanctions_birth_dates.csv") => birthDatesGraph(params: _*)
    case _ if url.endsWith("un_sc_sanctions_identifiers.csv") => identifiersGraph(params: _*)
    case _ if url.endsWith("un_sc_sanctions_nationalities.csv") => nationalitiesGraph(params: _*)
    case _ if url.endsWith("un_sc_sanctions.csv") => sanctionsGraph(params: _*)
  }
}
