package records.test

import org.scalatest._

// This is for 2.10.x compatibility!
import scala.language.reflectiveCalls
import scala.language.experimental.macros

object Typecheck {
  def typed(what: String): Unit = macro TypecheckingMacros.typed
  def typedWithMsg(what: String, msg: String): Unit = macro TypecheckingMacros.typedWithMsg
}

class VariousTests extends FlatSpec with Matchers {
  import Typecheck._

  def defRecord(age: Int = 2) = records.R(
    "age" -> age,
    "name" -> "David")


  "A Record" should "allow to read the value directly" in {
    val record = defRecord()

    record.age should be (2)
  }

  it should "be created with a special constructor" in {
    val row = records.R("foo" -> 1, ("bar", 2.3), Tuple2("baz", 1.7))

    row.foo should be (1)
    row.bar should be (2.3)
    row.baz should be (1.7)
  }

  it should "allow renaming in imports" in {
    import records.{ R => X }
    val row = X("foo" -> 1)

    row.foo should be (1)
  }

  it should "allow aliases" in {
    val X = records.R
    val row = X("foo" -> 1)

    row.foo should be (1)
  }

  it should "be hygienic" in {
    object records {
      val R = Predef
    }
    defRecord(3).age should be (3)
  }

  import records.R
  it should "allow strange field names" in {
    val record = R(
      "type" -> "R",
      "blank space" -> " ",
      "1" -> 1
    )

    record.`type` should be ("R")
    // record.`blank space` should be (" ") // does not work
    record.`1` should be (1)
  }

  it should "allow to read the value in a closure" in {
    val record = defRecord()

    val f = () => record.name

    f() should be ("David")
  }

  it should "allow Rows as a result in a method type" in {

    def query = defRecord()

    query.age should be (2)

  }

  it should "allow rows in generics" in {
    import language.existentials

    class Box[T](val x: T) {
      def get = x
    }

    val x = new Box(defRecord())

    x.get.age should be (2)

  }

  it should "allow to fill lists" in {
    val x = List.fill(1)(defRecord())

    x.head.age should be (2)
  }

  it should "LUB properly if both records are the same" in {
    val x = List(defRecord(), defRecord(3))

    x.head.age should be (2)
    x.last.age should be (3)

    val r = if (true) defRecord() else defRecord(3)
    val r1 = true match {
      case true => defRecord(3)
      case false => defRecord()
    }
    r.age should be (2)
    r1.age should be (3)
  }

  // Records have a curse that they can never be seen so
  // explicit mentions of records must be defined as case classes.
  case class AgeName(age: Int, name: String)

  it should "allow different valued rows in ascribed lists" in {
    val x = List[AgeName](defRecord().to[AgeName], defRecord(3).to[AgeName])
    x.head.age should be (2)
    x.last.age should be (3)
  }

  it should "allow to ascribe a result type" in {

    def query: AgeName =
      defRecord().to[AgeName]

    query.age should be (2)
  }

  it should "allow tuples to construct literal rows" in {

    val row = R(("foo", 1), ("bar", 2.3), Tuple2("baz", 1.7))

    row.foo should be (1)
    row.bar should be (2.3)
    row.baz should be (1.7)
  }

  it should "report an error on invalid field access" in {
    val row = records.R("foo" -> 1, ("bar", 2.3), Tuple2("baz", 1.7))

    typedWithMsg("""row.lol""",
      "value lol is not a member of records.R{def foo: Int; def bar: Double; def baz: Double}")
    // typed(""" row.foo """) // this will obviously compile
  }
}