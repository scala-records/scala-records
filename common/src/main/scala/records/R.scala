package records

trait R {
  protected val _row: List[Any]
  def row: List[Any] = _row
}
