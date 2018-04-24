package com.archerimpact.architect.keystone.shipments

import com.archerimpact.architect.keystone.parsers.Parser
import com.google.cloud.storage.{Blob, Storage, StorageOptions}

/* A shipment for Google Cloud Storage files */

class GoogleFile(val url: String,
                 val dataFormat: String,
                 val options: Map[String, String] = Map[String, String]()
                ) extends RawDataShipment {

  private val splitUrl = url.split("/").drop(2)
  val parser: Parser = chooseParser
  val data: Array[Byte] = loadData
  val country: String = splitUrl(1)
  val sourceName: String = splitUrl(2)

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
}
