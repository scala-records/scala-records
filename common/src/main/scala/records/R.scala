package records

import scala.language.experimental.macros

object R {
  def apply(v: (String, Any)*): R = macro records.Macros.apply_impl

  implicit class ConvertR[From <: R](val r: From) {
    import RecordConversions._
    def to[To](implicit conv: FromRecord[From, To]): To = conv(r)
  }
}

trait R {
  def data(fieldName: String): Any
}
