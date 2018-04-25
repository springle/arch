package com.archerimpact.architect.keystone.loaders

import com.archerimpact.architect.keystone.parsers.FakeParser
import com.archerimpact.architect.keystone.shipments.{FileURL, RawFile}

object FakeStorage extends Loader {
  override def fileURLtoRawFile(fileURL: FileURL): RawFile = new RawFile(
    url="fakeURL",
    fileFormat ="fakeFormat",
    data="fakeData".getBytes,
    country="fakeCountry",
    author="fakeSource",
    parser=new FakeParser
  )
}
