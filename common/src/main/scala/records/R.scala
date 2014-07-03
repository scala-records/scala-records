package records

object R {
  def apply(v: (String, Any)*): Any = macro records.Macros.apply_impl
}

trait R {
  protected val _data: List[Any]
  def data: List[Any] = _data
}
