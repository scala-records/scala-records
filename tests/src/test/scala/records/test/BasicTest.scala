package records.test

import org.scalatest._

// This is for 2.10.x compatibility!
import scala.language.reflectiveCalls

class BasicTest extends FlatSpec with Matchers {

  def defRecord(age: Int = 2) = records.R(
    "age" -> age,
    "name" -> "David")

  "A Record" should "allow to read the value directly" in {
    val record = defRecord()

    record.age should be(2)
  }

  it should "be created with a special constructor" in {
    val row = records.R("foo" -> 1, ("bar", 2.3), Tuple2("baz", 1.7))

    row.foo should be(1)
    row.bar should be(2.3)
    row.baz should be(1.7)
  }

  it should "allow renaming in imports" in {
    import records.{ R => X }
    val row = X("foo" -> 1)

    row.foo should be(1)
  }

  it should "allow aliases" in {
    val X = records.R
    val row = X("foo" -> 1)

    row.foo should be(1)
  }

  it should "be hygienic" in {
    object records {
      val R = Predef
    }
    defRecord(3).age should be(3)
  }

  import records.R
  it should "allow strange field names" in {
    val record = R(
      "type" -> "R",
      "blank space" -> "blank space",
      "1" -> 1,
      "1>2" -> "1>2",
      "豆贝尔维" -> "dòu bèi ěr wéi")

    record.`type` should be("R")
    record.`blank space` should be("blank space")
    record.`1` should be(1)
    record.`1>2` should be("1>2")
    record.`豆贝尔维` should be("dòu bèi ěr wéi")
  }

  it should "allow to read the value in a closure" in {
    val record = defRecord()

    val f = () => record.name

    f() should be("David")
  }

  it should "allow Rows as a result in a method type" in {

    def query = defRecord()

    query.age should be(2)

  }

  it should "allow rows in generics" in {
    import language.existentials

    class Box[T](val x: T) {
      def get = x
    }

    val x = new Box(defRecord())

    x.get.age should be(2)

  }

  it should "allow to fill lists" in {
    val x = List.fill(1)(defRecord())

    x.head.age should be(2)
  }

  it should "LUB properly if both records are the same" in {
    val x = List(defRecord(), defRecord(3))

    x.head.age should be(2)
    x.last.age should be(3)

    val r = if (true) defRecord() else defRecord(3)
    val r1 = true match {
      case true  => defRecord(3)
      case false => defRecord()
    }
    r.age should be(2)
    r1.age should be(3)
  }

  // Records have a curse that they can never be seen so
  // explicit mentions of records must be defined as case classes.
  case class AgeName(age: Int, name: String)

  it should "allow different valued rows in ascribed lists" in {
    val x = List[AgeName](defRecord().to[AgeName], defRecord(3).to[AgeName])
    x.head.age should be(2)
    x.last.age should be(3)
  }

  it should "allow to ascribe a result type" in {

    def query: AgeName =
      defRecord().to[AgeName]

    query.age should be(2)
  }

  it should "allow tuples to construct literal rows" in {

    val row = R(("foo", 1), ("bar", 2.3), Tuple2("baz", 1.7))

    row.foo should be(1)
    row.bar should be(2.3)
    row.baz should be(1.7)
  }

  it should "allow nested records" in {
    val x = R("a" -> R("b" -> R("c" -> 1)))

    x.a.b.c should be(1)
  }

}
