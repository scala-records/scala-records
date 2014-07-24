package records.test

import org.scalatest._

import records.Rec

class UnapplyTest extends FlatSpec with Matchers {
  "A record" should "extract fields with corresponding types" in {
    val Rec(x, y) = Rec("x" -> 1, "y" -> 2)
    val xx: Int = x
    val yy: Int = y
  }

  it should "be able to extract fields that are not in the signature" in {
    val Rec(x, y) = (Rec("x" -> 1, "y" -> 2): Rec)
  }

  it should "match any record with empty list of subpatterns" in {
    val Rec() = Rec("x" -> 1)
  }

  it should "match with subpatterns within binders" in {
    val Rec(x @ 1, y @ 2) = Rec("x" -> 1, "y" -> 2)
  }

  it should "fail to match if not all required fields are present" in {
    an[MatchError] should be thrownBy {
      val Rec(x, y) = Rec()
    }
  }

  it should "extract fields with unicode name" in {
    val Rec(π) = Rec("π" -> 3.14)
  }

  it should "interact nicely with type patterns" in {
    val Rec(x: Int) = Rec("x" -> 1)
  }
}
