package com.archerimpact.architect.keystone

import com.archerimpact.architect.keystone.parsers.Parser

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global
import com.google.cloud.storage.Storage
import com.google.cloud.storage.StorageOptions
import com.google.cloud.storage.Blob


object Shipment {
  def packageShipment(url: String, dataFormat: String): Future[Shipment] = url match {
    case `url` if url.startsWith("gs://") => Future { new GCSShipment(url, dataFormat) }
    case `url` if url.startsWith("dum://") => Future { new DummyShipment(url, dataFormat) }
  }
}

abstract class Shipment {
  val url: String                       // where is the data located (eg. "gs://archer-source-data/usa/ofac/sdn.csv")
  val dataFormat: String                // what format is it in (eg. "csv")
  val data: Any                         // the actual data
  val country: String                   // country of origin (eg. "usa")
  val sourceName: String                // name of source (eg. "ofac")
  val options: Map[String, String]      // options for interactive parsers
  val parser: Parser                    // instance of parser class for ParserActor to use
}

/* ----------------------------------- */
/* A Shipment for Google Cloud Storage */
/* ----------------------------------- */

class GCSShipment(val url: String,
                  val dataFormat: String,
                  val options: Map[String, String] = Map[String, String]()
                 ) extends Shipment {

  private val splitUrl = url.split("/").drop(2)

  private def loadData: Array[Byte] = {
    val bucketName = splitUrl(0)
    val blobPath = splitUrl.drop(1).mkString("/")
    val storage: Storage = StorageOptions.getDefaultInstance.getService
    val blob: Blob = storage.get(bucketName, blobPath)
    blob.getContent()
  }

  private def chooseParser: Parser = {
    val suffix = splitUrl.drop(1).dropRight(1).mkString(".")
    val parserLoc = "com.archerimpact.architect.keystone.parsers.countries." + suffix
    Class.forName(parserLoc).getConstructor().newInstance().asInstanceOf[Parser]
  }

  val data: Any = loadData
  val parser: Parser = chooseParser
  val country: String = splitUrl(1)
  val sourceName: String = splitUrl(2)
}

/* ---------------------------- */
/* A dummy Shipment for testing */
/* ---------------------------- */

class DummyShipment(
                     val url: String = "dum://my.source",
                     val dataFormat: String = "dummy",
                     val data: String = "dummyData",
                     val country: String = "dummy",
                     val sourceName: String = "dummySource",
                     val options: Map[String, String] = Map[String, String](),
                     val parser: Parser = new parsers.DummyParser
                   ) extends Shipment

