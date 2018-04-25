package com.archerimpact.architect

package object keystone {
  def getProtoParams(cc: AnyRef): Map[String, Any] = (Map[String, Any]() /: cc.getClass.getDeclaredFields) {
    (a, f) =>
      f.setAccessible(true)
      a + (f.getName -> f.get(cc))
  }
}
