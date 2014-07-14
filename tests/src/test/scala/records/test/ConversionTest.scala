package records.test

import org.scalatest._

import records.R

class ConversionTests extends FlatSpec with Matchers {

  case class SimpleVal(a: Int)
  case class ObjectVal(myObject: AnyRef)
  case class DBRecord(name: String, age: Int, location: String)

  "A Record" should "be able to convert into a case class" in {
    val x = R("a" -> 1)
    val y = x.to[SimpleVal]

    y.a should be(1)
  }

  it should "be able to convert to looser case classes" in {
    val x = R("myObject" -> "String")
    val y = x.to[ObjectVal]

    y.myObject should be("String")
  }

  it should "be able to convert to narrower case classes" in {
    val x = R("myObject" -> "String", "foo" -> "bar")
    val y = x.to[ObjectVal]

    y.myObject should be("String")
  }
}
