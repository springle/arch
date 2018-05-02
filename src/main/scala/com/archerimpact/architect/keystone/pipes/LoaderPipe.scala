package com.archerimpact.architect.keystone.pipes

import com.archerimpact.architect.keystone.PipeSpec
import com.archerimpact.architect.keystone.loaders.{FakeStorage, GoogleCloudStorage}
import com.archerimpact.architect.keystone.shipments.{FileShipment, UrlShipment}

class LoaderPipe extends PipeSpec {

  override type InType = UrlShipment
  override type OutType = FileShipment

  def load(fileURL: UrlShipment): FileShipment = fileURL match {
    case `fileURL` if fileURL.url.startsWith("gs://") => GoogleCloudStorage.urlToFile(fileURL)
    case `fileURL` if fileURL.url.startsWith("fake://") => FakeStorage.urlToFile(fileURL)
  }

  override def flow(input: UrlShipment): FileShipment = load(input)
}
