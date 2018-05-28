package com.archerimpact.architect.arch.parsers.countries.cz

import com.archerimpact.architect.arch.parsers.Parser
import com.archerimpact.architect.arch.parsers.formats.HTMLParser
import com.archerimpact.architect.arch.shipments.GraphShipment
import org.json4s._
import org.json4s.native.JsonMethods.{parse => parseJson}
import net.ruippeixotog.scalascraper.browser.{Browser, JsoupBrowser}
import net.ruippeixotog.scalascraper._


class czech_business_registry extends HTMLParser{

  val source = "Czech Business Registry"

  //TODO: implement everything

  override def htmlToGraph(htmlData: Browser#DocumentType, url: String): GraphShipment = {
    println(htmlData.toHtml)
    GraphShipment(null, null, url, source)
  }

}
