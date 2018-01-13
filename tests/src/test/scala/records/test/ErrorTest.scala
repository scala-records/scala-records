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
    val row = records.Rec("foo" -> 1, ("bar", 2.3), Tuple2("baz", 1.7))

    typedWithMsg(
      """row.lol""",
      "value lol is not a member of records.Rec[AnyRef{def foo: Int; def bar: Double; def baz: Double}]")
  }

  it should "report an error on duplicate fields" in {
    typedWithMsg(
      """records.Rec("a" -> 1, "a" -> "Hello World")""",
      "Field a is defined more than once.")
    typedWithMsg(
      """records.Rec("a" -> 1, "a" -> "Hello World", "b" -> 3, "b" -> 3.4)""",
      "Fields a, b are defined more than once.")
  }

  it should "report an error when non-literals are used" in {
    val x = 1
    val b = ("foo", 4)
    typedWithMsg(
      """records.Rec("a" -> x, b)""",
      "Records can only be constructed with tuples (a, b) and arrows a -> b.")
  }

  "Record Conversions" should "report an error if conversion to non-case class is attempted" in {
    val row = records.Rec("foo" -> 1, ("bar", 2.3), Tuple2("baz", 1.7))

    class A(foo: Int, bar: Double, baz: Double)

    typedWithMsg(
      "row.to[A]",
      "Records can only be converted to case classes; A is not a case class.")
  }

  it should "report an error if fields are missing" in {
    val row1 = records.Rec("foo" -> 1, ("bar", 2.3), Tuple2("baz", 1.7))
    val row2 = records.Rec()

    case class A(foo: Int, bar: Double, baz: Double, msg: String)

    typedWithMsg(
      "row1.to[A]",
      "Converting to A would require the source record to have the " +
        "following additional fields: [msg: String].")
    typedWithMsg(
      "row2.to[A]",
      "Converting to A would require the source record to have the " +
        "following additional fields: [foo: Int, bar: Double, baz: Double, msg: String].")
  }

  it should "report an error if fields have bad type" in {
    val row = records.Rec("foo" -> "Hello", "bar" -> 5)

    case class A(foo: Int, bar: Int)

    typedWithMsg(
      "row.to[A]",
      "Type of field foo: String of source record doesn't conform the expected type (Int).")
  }

  it should "report an error if conversion is attempted to multi-param-list case classes" in {
    val row = records.Rec("x" -> 1, "y" -> 1)

    case class A(x: Int, y: Int)(z: Int)

    typedWithMsg(
      "row.to[A]",
      "Target case class may only have a single parameter list.")
  }

  it should "report an error if we try to access incorrectly a nested record" in {
    import records.Rec
    val row = Rec("a" -> Rec("b" -> Rec("c" -> 1)))

    typedWithMsg(
      "row.a.c",
      "value c is not a member of records.Rec[AnyRef{def b: records.Rec[AnyRef{def c: Int}]}]")
  }

  it should "report an error when calling conversion method without type argument" in {
    import records._
    val record = Rec("field" -> "42")
    case class FieldHolder(field: String)

    typedWithMsg(
      "record.to",
      "Known limitation: Converting records requires an explicit type argument to `to` method representing the target case class")

    typedWithMsg(
      "val x: FieldHolder = record.to",
      "Known limitation: Converting records requires an explicit type argument to `to` method representing the target case class")
  }

  import records.RecordConversions._
  it should "report a nice error if inner fields are missing" in {
    import records.Rec
    case class A(a: B)
    case class B(b: C)
    case class C(c: Int, d: Int, x: Int)
    val row = Rec("a" -> Rec("b" -> Rec("k" -> 1, "d" -> 2)))

    typedWithMsg("row.to[A]", "Converting to A would require the source record " +
      "to have the following additional fields: [a.b.c: Int, a.b.x: Int].")
  }

  it should "report a nice error if a nested field is of wrong type" in {
    import records.Rec
    case class A(a: B)
    case class B(b: C)
    case class C(c: String)
    val row = Rec("a" -> Rec("b" -> Rec("c" -> 1)))

    typedWithMsg(
      "row.to[A]",
      "Type of field a.b.c: Int of source record doesn't conform the expected type (String).")
  }

  it should "report a nice error if the inner type is not a case class" in {
    import records.Rec
    case class A(a: B)
    case class B(b: Int)
    val row = Rec("a" -> Rec("b" -> Rec("k" -> 1, "d" -> 2)))

    typedWithMsg(
      "row.to[A]",
      "Type of field a.b: records.Rec[AnyRef{def k: Int; def d: Int}] of source record doesn't conform the expected type (Int).")
  }

  it should "report an error if a bad method is called on Rec" in {
    import records.Rec
    typedWithMsg(
      "Rec.foo()",
      "value foo is not a member of records.Rec")
  }

  it should "report an error if a bad method is called on Rec (named param case)" in {
    import records.Rec
    typed("Rec.foo(foo = 1)")
  }

  it should "report an error if Rec.invokeDynamic is called with a variable method name" in {
    import records.Rec
    val x = "apply"
    typedWithMsg(
      "Rec.applyDynamic(x)()",
      "You may not invoke Rec.applyDynamic with a non-literal method name.")
  }
}
