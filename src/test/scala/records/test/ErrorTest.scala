package records.test

import org.scalatest._

// This is for 2.10.x compatibility!
import scala.language.reflectiveCalls

object Typecheck {
  import scala.language.experimental.macros

  def typed(what: String): Unit = macro TypecheckingMacros.typed
  def typedWithMsg(what: String, msg: String): Unit = macro TypecheckingMacros.typedWithMsg
}

class ErrorTests extends FlatSpec with Matchers {
  import Typecheck._

  "A record" should "report an error on invalid field access" in {
    val row = records.R("foo" -> 1, ("bar", 2.3), Tuple2("baz", 1.7))

    typedWithMsg("""row.lol""",
      "value lol is not a member of records.R{def foo: Int; def bar: Double; def baz: Double}")
  }

  it should "report an error on duplicate fields" in {
    typedWithMsg("""records.R("a" -> 1, "a" -> "Hello World")""", "Field a is defined more than once.")
    typedWithMsg("""records.R("a" -> 1, "a" -> "Hello World", "b" -> 3, "b" -> 3.4)""",
      "Fields a, b are defined more than once.")
  }

  it should "report an error when non-literals are used" in {
    val x = 1
    val b = ("foo", 4)
    typedWithMsg("""records.R("a" -> x, b)""",
      "Records can only be constructed with tuples (a, b) and arrows a -> b.")
  }

  "Record Conversions" should "report an error if conversion to non-case class is attempted" in {
    val row = records.R("foo" -> 1, ("bar", 2.3), Tuple2("baz", 1.7))

    class A(foo: Int, bar: Double, baz: Double)

    typedWithMsg("row.to[A]",
      "Records can only be converted to case classes; A is not a case class.")
  }

  it should "report an error if fields are missing" in {
    val row1 = records.R("foo" -> 1, ("bar", 2.3), Tuple2("baz", 1.7))
    val row2 = records.R()

    case class A(foo: Int, bar: Double, baz: Double, msg: String)

    typedWithMsg("row1.to[A]",
      "Converting to A would require the source record to have the " +
        "following additional fields: [msg].")
    typedWithMsg("row2.to[A]",
      "Converting to A would require the source record to have the " +
        "following additional fields: [foo, bar, baz, msg].")
  }

  it should "report an error if fields have bad type" in {
    val row = records.R("foo" -> "Hello", "bar" -> 5)

    case class A(foo: Int, bar: Int)

    typedWithMsg("row.to[A]",
      "Type of field foo of source record (String) doesn't conform the expected type (Int).")
  }

  it should "report an error if conversion is attempted to multi-param-list case classes" in {
    val row = records.R("x" -> 1, "y" -> 1)

    case class A(x: Int, y: Int)(z: Int)

    typedWithMsg("row.to[A]",
      "Target case class may only have a single parameter list.")
  }

}
