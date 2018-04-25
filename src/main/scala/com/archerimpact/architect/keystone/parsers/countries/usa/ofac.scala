package com.archerimpact.architect.keystone.parsers.countries.usa

import com.archerimpact.architect.keystone.parsers.formats.CSV
import com.archerimpact.architect.keystone.shipments.Entity
import com.archerimpact.architect.ontology.OFACSanction

class ofac extends CSV {

  override def mkEntity(params: String *): Option[Entity] = params match {
    case Seq(entNum, sdnName, sdnType, program, title, callSign, vesselType, tonnage, grt, vesselFlag, vesselOwner, remarks) =>
      Some(Entity(entNum, OFACSanction(entNum, sdnName, sdnType, program, title, callSign, vesselType, tonnage, grt, vesselFlag, vesselOwner, remarks)))
    case _ => None
  }

}

