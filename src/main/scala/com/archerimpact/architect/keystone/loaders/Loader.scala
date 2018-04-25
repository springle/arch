package com.archerimpact.architect.keystone.loaders

import com.archerimpact.architect.keystone.shipments.{UrlShipment, FileShipment}

trait Loader {
  def urlToFile(url: UrlShipment): FileShipment
}
