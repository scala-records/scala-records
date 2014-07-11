package records

import scala.language.experimental.macros

import scala.reflect._

object R {
  def apply(v: (String, Any)*): R = macro records.Macros.apply_impl

  implicit class ConvertR[From <: R](val record: From) extends AnyVal {
    def to[To]: To = macro RecordConversions.fromRecord_impl[From, To]
  }
}

/**
 * Base class for all record types. The __data* members should only
 * be defined and referred to by macro implementations.
 * The specialized versions should be preferred in both cases. A
 * concrete implementation of R (generated by a macro) must provide
 * overrides for either [[__data]] or all __data* members.
 *
 * Callers may choose either of [[__data]] or the specialized
 * versions, but should prefer the specialized versions whenever
 * possible to avoid boxing.
 */
trait R {
  @inline def __data[T: ClassTag](fieldName: String): T = {
    val res = classTag[T] match {
      case ClassTag.Boolean => __dataBoolean(fieldName)
      case ClassTag.Byte    => __dataByte(fieldName)
      case ClassTag.Short   => __dataShort(fieldName)
      case ClassTag.Char    => __dataChar(fieldName)
      case ClassTag.Int     => __dataInt(fieldName)
      case ClassTag.Long    => __dataLong(fieldName)
      case ClassTag.Float   => __dataFloat(fieldName)
      case ClassTag.Double  => __dataDouble(fieldName)
      case _                => __dataObj[T](fieldName)
    }
    res.asInstanceOf[T]
  }

  @inline def __dataObj[T: ClassTag](fieldName: String): T = __data(fieldName)
  @inline def __dataBoolean(fieldName: String): Boolean = __data(fieldName)
  @inline def __dataByte(fieldName: String): Byte = __data(fieldName)
  @inline def __dataShort(fieldName: String): Short = __data(fieldName)
  @inline def __dataChar(fieldName: String): Char = __data(fieldName)
  @inline def __dataInt(fieldName: String): Int = __data(fieldName)
  @inline def __dataLong(fieldName: String): Long = __data(fieldName)
  @inline def __dataFloat(fieldName: String): Float = __data(fieldName)
  @inline def __dataDouble(fieldName: String): Double = __data(fieldName)
}
