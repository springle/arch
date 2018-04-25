package com.archerimpact.architect

import com.archerimpact.architect.keystone.shipments.{Entity, Graph}

package object keystone {
  def getProtoParams(cc: AnyRef): Map[String, Any] = (Map[String, Any]() /: cc.getClass.getDeclaredFields) {
    (a, f) =>
      f.setAccessible(true)
      a + (f.getName -> f.get(cc))
  }
  def getProtoType(cc: AnyRef): String = cc.getClass.getName.split("\\.").last
  def getArchitectId(entity: Entity, graph: Graph): String = s"${graph.url}/${entity.id}"
}
