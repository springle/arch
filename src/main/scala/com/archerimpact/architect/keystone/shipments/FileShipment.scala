package com.archerimpact.architect.keystone.shipments

import com.archerimpact.architect.keystone.parsers.Parser

class FileShipment(
               val url: String,          // Where did the file come from? (eg. "gs://archer-source-data/usa/ofac/sdn.csv")
               val fileFormat: String,   // What format is the file in? (eg. "csv")
               val data: Array[Byte],    // Put the actual data here.
               val country: String,      // What country is the file from? (eg. "usa")
               val author: String,       // Who produced the file? (eg. "ofac")
               val parser: Parser,       // Choose a parser to process this file.
             ) extends Shipment
