package com.archerimpact.architect.arch

import scala.reflect.runtime.{universe=>ru}

package object parsers {
  def getTypeTag[T: ru.TypeTag](obj: T): ru.TypeTag[T] = ru.typeTag[T]
}
