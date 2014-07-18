package records.test

import org.scalatest._

import records.Rec

// This is for 2.10.x compatibility!
import scala.language.reflectiveCalls

class ConversionTests extends FlatSpec with Matchers {

  case class SimpleVal(a: Int)
  case class ObjectVal(myObject: AnyRef)
  case class DBRecord(name: String, age: Int, location: String)
  case class DBRecordHolder(f: DBRecord, anything: Any)

  "A Record" should "be able to convert into a case class" in {
    val x = Rec("a" -> 1)
    val y = x.to[SimpleVal]

    y.a should be(1)
  }

  it should "be able to convert to looser case classes" in {
    val x = Rec("myObject" -> "String")
    val y = x.to[ObjectVal]

    y.myObject should be("String")
  }

  it should "be able to convert to narrower case classes" in {
    val x = Rec("myObject" -> "String", "foo" -> "bar")
    val y = x.to[ObjectVal]

    y.myObject should be("String")
  }

  it should "allow conversion if there is a `to` field" in {
    val record = Rec("to" -> "R")
    case class ToHolder(to: String)

    record.to should be("R")
    new Rec.Convert(record).to[ToHolder] should be(ToHolder("R"))
  }

  import records.RecordConversions._
  it should "allow explicit conversion even when implicit conversion is imported" in {
    val record = Rec("field" -> "42")
    case class FieldHolder(field: String)

    record.to[FieldHolder] should be(FieldHolder("42"))
  }

  it should "implicitly convert to a case class in a val position" in {
    val x: DBRecord = Rec("name" -> "David", "age" -> 3, "location" -> "Lausanne")

    x.name should be("David")
  }

  it should "implicitly convert to a case class when constructing a list" in {
    val xs = List[DBRecord](
      Rec("name" -> "David", "age" -> 2, "location" -> "Lausanne"),
      Rec("name" -> "David", "age" -> 3, "location" -> "Lausanne"))

    xs.head.name should be("David")
    xs.tail.head.name should be("David")
  }

  it should "with nested records explicitly convert to a case class" in {
    val rec = Rec("f" -> Rec("name" -> "David", "age" -> 2, "location" -> "Lausanne"),
      "anything" -> 1)

    rec.to[DBRecordHolder].f.name should be("David")
  }

  it should "with nested records implicitly convert to a case class" in {
    val xs = List[DBRecordHolder](
      Rec("f" -> Rec("name" -> "David", "age" -> 2, "location" -> "Lausanne"),
        "anything" -> 1),
      Rec("f" -> Rec("name" -> "David", "age" -> 3, "location" -> "Lausanne"),
        "anything" -> 1))

    xs.head.f.name should be("David")
    xs.tail.head.f.name should be("David")
  }
}
