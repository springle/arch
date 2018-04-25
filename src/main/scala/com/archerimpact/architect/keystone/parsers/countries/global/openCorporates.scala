package com.archerimpact.architect.keystone.parsers.countries.global

import com.archerimpact.architect.keystone.Graph
import com.archerimpact.architect.keystone.parsers.formats.JSON

class openCorporates extends JSON {
  override def parse(data: Array[Byte], url: String): Graph = {


    



    Graph(List(),List(), url)
  }
}
