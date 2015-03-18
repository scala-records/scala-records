package records.test

import org.scalatest._

import records.Rec

class UnapplyTest extends FlatSpec with Matchers {
  "A record" should "extract fields with corresponding types" in {
    val Rec(x, y) = Rec(x = 1, y = 2)
    val xx: Int = x
    val yy: Int = y
  }

  it should "be able to extract fields that are not in the signature" in {
    val rec: Rec[_] = Rec(x = 1, y = 2)
    val Rec(x, y) = rec
    val Rec(("x", _), ("y", _)) = rec
  }

  it should "match any record with empty list of subpatterns" in {
    val Rec() = Rec(x = 1)
  }

  it should "match with subpatterns within binders" in {
    val Rec(x @ 1, y @ 2) = Rec(x = 1, y = 2)
    val Rec(("x", 1), ("y", 2)) = Rec(x = 1, y = 2)
  }

  it should "fail to match if not all required fields are present" in {
    an[MatchError] should be thrownBy {
      val Rec(x, y) = Rec()
    }
  }

  it should "extract fields with unicode name" in {
    val rec = Rec(`π` = 3.14)
    val Rec(π) = rec
    val Rec(("π", 3.14)) = rec
  }

  it should "interact nicely with type patterns" in {
    val Rec(x: Int) = Rec(x = 1)
  }

  it should "be able to cross-match records with the same field name" in {
    val p1 = Rec(x = 1, y = 2)
    val p2 = Rec(x = 2, y = 2)
    (p1, p2) match {
      case (Rec(("y", y1)), Rec(("y", y2))) if y1 == y2 =>
    }
  }
}
