package com.archerimpact.architect.arch.loaders

import com.archerimpact.architect.arch.parsers.Parser
import com.archerimpact.architect.arch.shipments.{UrlShipment, FileShipment}
import com.google.cloud.storage.{Blob, Storage, StorageOptions}

object GoogleCloudStorage extends Loader {
  override def urlToFile(fileURL: UrlShipment): FileShipment = {
    val splitURL: Array[String] = fileURL.url.split("/").drop(2)
    new FileShipment(
      url=fileURL.url,
      format=fileURL.fileFormat,
      data=loadData(splitURL),
      country=splitURL(1),
      author=splitURL(2),
      parser=chooseParser(splitURL)
    )
  }

  private def loadData(splitURL: Array[String]): Array[Byte] = {
    val bucketName = splitURL(0)
    val blobPath = splitURL.drop(1).mkString("/")
    val storage: Storage = StorageOptions.getDefaultInstance.getService
    val blob: Blob = storage.get(bucketName, blobPath)
    blob.getContent()
  }

  private def chooseParser(splitURL: Array[String]): Parser = {
    val suffix = splitURL.drop(1).dropRight(1).mkString(".")
    val parserLoc = "com.archerimpact.architect.arch.parsers.countries." + suffix
    Class.forName(parserLoc).getConstructor().newInstance().asInstanceOf[Parser]
  }
}
