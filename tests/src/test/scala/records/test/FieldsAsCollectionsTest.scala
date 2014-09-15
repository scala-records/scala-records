package records.test

import org.scalatest._

// // This is for 2.10.x compatibility!
// import scala.language.reflectiveCalls

class FieldsAsCollectionsTest extends FlatSpec with Matchers {
  import records.Rec

  // Lists of primitives
  "A Record" should "be able to have List[Int]s as fields" in {
    val a = Rec("xs" -> List(1, 2, 3))

    a.xs should be(List(1, 2, 3))
  }

  it should "be able to have List[Doubles]s as fields" in {
    val a = Rec("xs" -> List(1.0, 2.0, 3.0))

    a.xs should be(List(1.0, 2.0, 3.0))
  }

  it should "be able to have List[Byte]s as fields" in {
    val a = Rec("xs" -> List(1.toByte, 2.toByte, 3.toByte))

    a.xs should be(List(1.toByte, 2.toByte, 3.toByte))
  }

  it should "be able to have List[Short]s as fields" in {
    val a = Rec("xs" -> List(1.toShort, 2.toShort, 3.toShort))

    a.xs should be(List(1.toShort, 2.toShort, 3.toShort))
  }

  it should "be able to have List[Long]s as fields" in {
    val a = Rec("xs" -> List(1L, 2L, 3L))

    a.xs should be(List(1L, 2L, 3L))
  }

  it should "be able to have List[Float]s as fields" in {
    val a = Rec("xs" -> List(1F, 2F, 3F))

    a.xs should be(List(1F, 2F, 3F))
  }

  it should "be able to have List[Boolean]s as fields" in {
    val a = Rec("xs" -> List(true, true, false))

    a.xs should be(List(true, true, false))
  }

  it should "be able to have List[String]s as fields" in {
    val a = Rec("xs" -> List("one", "two", "three"))

    a.xs should be(List("one", "two", "three"))
  }

  // Maps of primitives
  import scala.collection.immutable.HashMap

  it should "be able to have Map[String, Int]s as fields" in {
    val a = Rec("map" -> HashMap("one" -> 1, "two" -> 2))

    a.map("one") should be(1)
    a.map("two") should be(2)
  }

  it should "be able to have Map[String, Array[Int]]s as fields" in {
    val a = Rec("map" -> HashMap("one" -> Array(1, 1, 1), "two" -> Array(2, 2, 2)))

    a.map("one") should be(Array(1, 1, 1))
    a.map("two") should be(Array(2, 2, 2))
  }

  // Fields with collections of collections
  it should "be able to have List[List[Int]]s as fields" in {
    val a = Rec("xs" -> List(List(1), List(2)))

    a.xs.head.head should be(1)
    a.xs.tail.head.head should be(2)
  }

  it should "be able to have List[Map[String, Int]]s as fields" in {
    val a = Rec("xs" -> List(HashMap("one" -> 1, "two" -> 2), HashMap("three" -> 3, "four" -> 4)))

    a.xs.head("one") should be(1)
    a.xs.head("two") should be(2)
    a.xs.tail.head("three") should be(3)
    a.xs.tail.head("four") should be(4)
  }

}
