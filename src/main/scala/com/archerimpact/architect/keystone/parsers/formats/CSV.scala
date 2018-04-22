package com.archerimpact.architect.keystone.parsers.formats

import com.archerimpact.architect.keystone.Graph
import com.archerimpact.architect.keystone.parsers.Parser

class CSV extends Parser {

  override def parse(data: Any, url: String): Graph = {
    Graph(List(), List(), url)
  }

}
