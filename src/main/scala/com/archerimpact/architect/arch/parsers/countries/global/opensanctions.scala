package com.archerimpact.architect.arch.parsers.countries.global

import com.archerimpact.architect.arch.parsers.formats.CSVParser
import com.archerimpact.architect.arch.shipments.{Entity, Link, PartialGraph}
import com.archerimpact.architect.ontology._

class opensanctions extends CSVParser {
  val source = "Amnesty DVC"

  def birthDatesGraph(params: String*): PartialGraph = PartialGraph()  // TODO: integrate without overwriting
  def nationalitiesGraph(params: String*): PartialGraph = PartialGraph()  // TODO: integrate without overwriting

  def addressesGraph(params: String*): PartialGraph = params match {
    case Seq(
      city, id, lastSeen,
      text, region, note,
      street, postalCode, countryCode,
      countryName, street2, firstSeen
    ) =>
      val combined = List(street, street2, null, city, region, postalCode, countryName).
        filter(field => field != null).
        mkString(", ")
      val entities =
        if (combined.toString.count(c => c == ',') > 1)  // filter out non-specific addresses
          List(Entity(combined, address(
            combined, street, street2,
            "", city, region,
            postalCode, countryName
          )))
        else
          List[Entity]()
      val links = List(Link(id, "HAS_KNOWN_LOCATION", combined))
      PartialGraph(entities, links)
  }

  def aliasesGraph(params: String*): PartialGraph = params match {
    case Seq(
      firstName, lastName, description,
      quality, title, thirdName,
      lastSeen, id, fatherName,
      entityType, secondName, name,
      firstSeen
    ) =>
      val entity = Entity(s"$id/aka/$name", person(name, notes = "This entity might be an organization."))
      val link = Link(id, "AKA", s"$id/aka/$name")
      PartialGraph(List(entity), List(link))
  }

  def identifiersGraph(params: String*): PartialGraph = params match {
    case Seq(
      id, description, lastSeen,
      number, countryName, countryCode,
      issuedAt, entityType, firstSeen
    ) =>
      val proto = identifyingDocument(
        number, entityType, countryName,
        countryName, issuedOn=issuedAt, notes=description
      )
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
        val mainEntity = Entity(id, entityType match {
          case "individual" => person(name=name, titles=List(title), notes=summary)
          case "entity" => organization(name=name, notes=summary)
        })
        val combinedName = List(firstName, secondName, thirdName).filter(name => name != null).mkString(" ")
        val akaEntity = Entity(s"$id/aka", entityType match {
          case "individual" => person(combinedName)
          case "entity" => organization(combinedName)
        })
        val akaLink = Link(id, "AKA", s"$id/aka")
        val entities = List(mainEntity, akaEntity)
        val links = List(akaLink)
        PartialGraph(entities, links)
  }

  def nqCoGraph(params: String*): PartialGraph = params match {
    case Seq(coID, raLinkID, name, countryOfOperation, countryOfOrigin, industrySector, businessType,
    website, directorNames, accusedOf, connectionDesc, workerID, companyAddress, sisterCompany, jointVenture, phoneNumber, crNumber, aliases) =>
    {
      if (name==null || name=="") {
        return PartialGraph(List[Entity](), List[Link]())
      }

      val coEntity = List(Entity(coID, company(name=name, countryOfOperation=countryOfOperation, countryOfOrigin=countryOfOrigin,
        industrySector=industrySector, businessType=businessType, website=website, accusedOf=accusedOf, companyAddress=companyAddress,
        sisterCompany=sisterCompany, jointVenture=jointVenture, phoneNumber=phoneNumber, crNumber=crNumber, aliases=aliases)))

      val directorEntities = directorNames match {
        case null => List[Entity]()
        case "null" => List[Entity]()
        case _ => List(Entity(s"$coID/director", person(name=directorNames)))
      }

      val raEntities = raLinkID match {
        case null => List[Entity]()
        case "null" => List[Entity]()
        case _ => {
          val cleanedRaID = raLinkID.replace(" ", "")
          List(Entity(cleanedRaID, recruitingAgency(notes=cleanedRaID)))
        }
      }

      val dirLinks = directorEntities.map(dir => Link(dir.id, "director_of", coID))
      val raLinks = raEntities.map(ra => Link(coID, "connected_to", ra.id))

      val allEntities = coEntity ++ directorEntities ++ raEntities
      val allLinks = raLinks ++ dirLinks

      PartialGraph(allEntities, allLinks)
    }
  }

  def nqRAGraph(params: String*): PartialGraph = params match {
    case Seq(raID, otherIDRange, name, governmentIDNumber, faxNumber, locationOfOrigin, locationOfOperation,
    numberOfWorkersSentAbroad, directorNames, recruitmentSector, yearsInOperation, accusedOf, webSources, archiveSources,
    generalSource, workerIDs, agencyAddress, other1, other2, other3) =>
    {
      if (name==null || name=="") {
        return PartialGraph(List[Entity](), List[Link]())
      }

      //this protobuff creation is fucked up somehow
      val coEntity = List(Entity(raID, recruitingAgency(notes=raID, name=name, governmentIDNumber=governmentIDNumber, faxNumber=faxNumber,
        locationOfOrigin=locationOfOrigin, locationOfOperation=locationOfOperation, numberOfWorkersSentAbroad=numberOfWorkersSentAbroad,
        recruitmentSector=recruitmentSector, yearsInOperation=yearsInOperation, accusedOf=accusedOf, webSources=webSources,
        archiveSources=archiveSources, agencyAddress=agencyAddress)))

      val directorEntities = directorNames match {
        case null => List[Entity]()
        case "null" => List[Entity]()
        case _ => List(Entity(s"$raID/director", person(name=directorNames)))
      }

      val dirLinks = directorEntities.map(dir => Link(dir.id, "director_of", raID))

      val allEntities = coEntity ++ directorEntities
      val allLinks = dirLinks

      PartialGraph(allEntities, allLinks)
    }
  }

  override def mkGraph(url: String, params: String*): PartialGraph = url match {
//    case _ if url.endsWith("un_sc_sanctions_addresses.csv") => addressesGraph(params: _*)
//    case _ if url.endsWith("un_sc_sanctions_aliases.csv") => aliasesGraph(params: _*)
//    case _ if url.endsWith("un_sc_sanctions_birth_dates.csv") => birthDatesGraph(params: _*)
//    case _ if url.endsWith("un_sc_sanctions_identifiers.csv") => identifiersGraph(params: _*)
//    case _ if url.endsWith("un_sc_sanctions_nationalities.csv") => nationalitiesGraph(params: _*)
//    case _ if url.endsWith("un_sc_sanctions.csv") => sanctionsGraph(params: _*)

    case _ if url.endsWith("un_sc_sanctions_addresses.csv") => PartialGraph(List(), List())
    case _ if url.endsWith("un_sc_sanctions_aliases.csv") => PartialGraph(List(), List())
    case _ if url.endsWith("un_sc_sanctions_birth_dates.csv") => PartialGraph(List(), List())
    case _ if url.endsWith("un_sc_sanctions_identifiers.csv") => PartialGraph(List(), List())
    case _ if url.endsWith("un_sc_sanctions_nationalities.csv") => PartialGraph(List(), List())
    case _ if url.endsWith("un_sc_sanctions.csv") => PartialGraph(List(), List())
    case _ if url.endsWith("nq-people.csv") => {
      PartialGraph(List(), List())
    }
    case _ if url.endsWith("nq-companies.csv") => {
      nqCoGraph(params: _*)
    }
    case _ if url.endsWith("nq-agencies.csv") => {
      nqRAGraph(params: _*)
    }

  }
}
