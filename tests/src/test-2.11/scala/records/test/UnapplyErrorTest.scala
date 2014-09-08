package records.test

import org.scalatest._

class UnapplyErrorTests extends FlatSpec with Matchers {
  import Typecheck._

  it should "report that unapply is not supported in expr mode" in {
    import records.Rec
    val r = Rec()
    typedWithMsg("Rec.unapply(r)",
      "Rec.unapply only works in pattern matching mode")
  }

  it should "report that subpatterns of rec must be bindings" in {
    import records.Rec
    val r = Rec("a" -> 1)
    typedWithMsg("r match { case Rec(1) => }",
      "Record field matcher must be a variable binding")
  }
}
