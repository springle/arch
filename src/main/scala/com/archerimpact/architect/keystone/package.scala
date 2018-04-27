package com.archerimpact.architect

import com.archerimpact.architect.keystone.loaders.GoogleCloudStorage
import com.archerimpact.architect.keystone.shipments.{Entity, FileShipment, GraphShipment, UrlShipment}

package object keystone {
  val excludedFields: Set[String] = Set("serialVersionUID")
  def protoParams(proto: AnyRef): Map[String, Any] = (Map[String, Any]() /: proto.getClass.getDeclaredFields) {
    (a, f) =>
      f.setAccessible(true)
      a + (f.getName -> f.get(proto))
  }.filterNot(_._1.startsWith("_")).filterNot(x => excludedFields.contains(x._1))
  def typeName(x: AnyRef): String = x.getClass.getName.split("\\.").last
  def architectId(entity: Entity, graph: GraphShipment): String = s"${graph.url}/${entity.id}"
  def loadFromGoogle(url: String): FileShipment = GoogleCloudStorage.urlToFile(new UrlShipment(url))
}
