package com.archerimpact.architect.arch.parsers.formats

import com.archerimpact.architect.arch.parsers.Parser
import com.archerimpact.architect.arch.shipments.GraphShipment
import net.ruippeixotog.scalascraper.browser.{Browser, JsoupBrowser}
import net.ruippeixotog.scalascraper._
import org.json4s._
import org.json4s.native.JsonMethods.{parse => parseJson}

abstract class HTMLParser extends Parser {

  def htmlToGraph(data: Browser#DocumentType, url: String): GraphShipment

  override def parse(data: Array[Byte], url: String): GraphShipment = {
    val htmlStr = data.map(_.toChar).mkString
    var browser = JsoupBrowser()
    var doc = browser.parseString(htmlStr)
    htmlToGraph(doc, url)
  }

}
