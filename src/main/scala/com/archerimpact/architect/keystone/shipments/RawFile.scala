package com.archerimpact.architect.keystone.shipments

import com.archerimpact.architect.keystone.parsers.Parser

class RawFile(
               val url: String,                     // Where did the file come from? (eg. "gs://archer-source-data/usa/ofac/sdn.csv")
               val dataFormat: String,              // What format is the file in? (eg. "csv")
               val data: Array[Byte],               // Put the actual data here.
               val country: String,                 // What country is the file from? (eg. "usa")
               val sourceName: String,              // Who produced the file? (eg. "ofac")
               val options: Map[String, String],    // Pass options to the parser here.
               val parser: Parser,                  // Choose a parser to process this file.
             ) extends Shipment
