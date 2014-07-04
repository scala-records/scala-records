package records

object R {
  def apply(v: (String, Any)*): Any = macro records.Macros.apply_impl
}

trait R {
  def data(fieldName: String): Any
}
