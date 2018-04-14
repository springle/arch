package com.archerimpact.architect.pipeline

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

object Shipment {
  def packageShipment(url: String, dataFormat: String): Future[Shipment] = url match {
    case `url` if url.startsWith("gs://") => Future { new GCSShipment(url, dataFormat) }
    case `url` if url.startsWith("dum://") => Future { new DummyShipment(url, dataFormat) }
  }
}

abstract class Shipment {
  val url: String                                // where is the data located
  val dataFormat: String                         // what format is it in
  val data: Any                                  // the actual data
}

/* ----------------------------------- */
/* A Shipment for Google Cloud Storage */
/* ----------------------------------- */

class GCSShipment(
                 val url: String,
                 val dataFormat: String
                 ) extends Shipment {
  def loadData = "test"  // TODO: actually load from GCS
  val data: Any = loadData
}

/* ---------------------------- */
/* A dummy Shipment for testing */
/* ---------------------------- */

class DummyShipment(
                     val url: String = "dum://my.source",
                     val dataFormat: String = "dummy",
                     val data: String = "dummyData"
                   ) extends Shipment

