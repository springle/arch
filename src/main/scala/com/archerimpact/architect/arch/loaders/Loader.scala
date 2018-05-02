package com.archerimpact.architect.arch.loaders

import com.archerimpact.architect.arch.shipments.{UrlShipment, FileShipment}

trait Loader {
  def urlToFile(url: UrlShipment): FileShipment
}
