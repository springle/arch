package com.archerimpact.architect

import com.archerimpact.architect.keystone.shipments.{Entity, GraphShipment}

package object keystone {
  def getProtoParams(cc: AnyRef): Map[String, Any] = (Map[String, Any]() /: cc.getClass.getDeclaredFields) {
    (a, f) =>
      f.setAccessible(true)
      a + (f.getName -> f.get(cc))
  }.filterNot(_._1.startsWith("_"))
  def getProtoType(cc: AnyRef): String = cc.getClass.getName.split("\\.").last
  def architectId(entity: Entity, graph: GraphShipment): String = s"${graph.url}/${entity.id}"
}
