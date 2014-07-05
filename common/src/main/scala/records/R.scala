package records

import scala.language.experimental.macros

object R {
  def apply(v: (String, Any)*): R = macro records.Macros.apply_impl
}

trait R {
  def data(fieldName: String): Any
}
