package com.archerimpact.architect.arch.parsers.formats

import com.archerimpact.architect.arch.parsers.Parser
import com.archerimpact.architect.arch.shipments.GraphShipment
import net.ruippeixotog.scalascraper.browser.JsoupBrowser
import org.json4s._
import org.json4s.native.JsonMethods.{parse=>parseJson}

abstract class HTMLParser extends Parser {

  def htmlToGraph(data: String, url: String): GraphShipment

  override def parse(data: Array[Byte], url: String): GraphShipment = {
    val htmlStr = data.map(_.toChar).mkString
    htmlToGraph(htmlStr, url)
  }

}
