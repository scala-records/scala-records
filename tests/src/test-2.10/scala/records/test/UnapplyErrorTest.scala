package records.test

import org.scalatest._

import scala.language.reflectiveCalls

class UnapplyErrorTests extends FlatSpec with Matchers {
  import Typecheck._

  it should "report that unapply is not supported on 2.10" in {
    import records.Rec
    val r = Rec()
    typedWithMsg("Rec.unapply(r)",
      "Record matching is not supported on 2.10.x")
  }
}
