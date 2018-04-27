package com.archerimpact.architect.keystone.loaders

import com.archerimpact.architect.keystone.parsers.FakeParser
import com.archerimpact.architect.keystone.shipments.{UrlShipment, FileShipment}

object FakeStorage extends Loader {
  override def urlToFile(fileURL: UrlShipment): FileShipment = new FileShipment(
    url="fakeURL",
    format ="fakeFormat",
    data="fakeData".getBytes,
    country="fakeCountry",
    author="fakeSource",
    parser=new FakeParser
  )
}
