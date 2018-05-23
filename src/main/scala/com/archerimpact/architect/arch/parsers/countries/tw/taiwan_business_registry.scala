package com.archerimpact.architect.arch.parsers.countries.tw

import com.archerimpact.architect.arch.parsers.Parser
import com.archerimpact.architect.arch.parsers.formats.HTMLParser
import com.archerimpact.architect.arch.shipments.GraphShipment
import org.json4s._
import org.json4s.native.JsonMethods.{parse => parseJson}
import net.ruippeixotog.scalascraper.browser.JsoupBrowser



class taiwan_business_registry extends HTMLParser{

  val source = "Taiwan Business Registry"

  override def htmlToGraph(data: String, url: String): GraphShipment = {
    println(data)
    GraphShipment(null, null, url, source)
  }

}
