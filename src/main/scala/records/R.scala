package records

import scala.language.experimental.macros

object R {
  def apply(v: (String, Any)*): R = macro records.Macros.apply_impl

  implicit class ConvertR[From <: R](val record: From) extends AnyVal {
    def to[To]: To = macro RecordConversions.fromRecord_impl[From, To]
  }
}

trait R {
  def __data[T](fieldName: String): T
}
