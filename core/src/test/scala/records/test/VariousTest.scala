package records.test

import org.scalatest._

import records.R

class VariousTests extends FlatSpec with Matchers {

  def defRecord(age: Int = 2) = R(
    "age" -> age,
    "name" -> "David",
    "type" -> "R",
    "blank space" -> " ",
    "1" -> 1)


  "A Record" should "allow to read the value directly" in {
    val record = defRecord()

    record.age should be (2)
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

  it should "LUB properly" in {
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

  // this is defined by the user. Could also be a case class!
  trait VHolder { def age: Int }

  // it would be good if a generic macro could take care of this for all return types!
  import scala.language.implicitConversions
  implicit def rowToEntity[T <: R](x: T): VHolder = {
    // Age is in field with index 1, since we reorder fields by name
    new VHolder { def age = x.data("age").asInstanceOf[Int] }
  }

  it should "allow different valued rows in ascribed lists" in {
    // This would be too cool, but it wont work.
    // val x = List[R { def v: Any }](defRecord, rec(List(2)))

    // Lets ask the user to provide a case class for each explicit case.
    val x = List[VHolder](defRecord(), defRecord(3))
    x.head.age should be (2)
    x.last.age should be (3)
  }

  it should "allow to ascribe a result type" in {

    def query: VHolder =
      defRecord()

    query.age should be (2)
  }

}

  // possible record operations
//  it should "be possible to pattern match on records" in {
//    val rec = R {val v = 1; val x = "foo";}
//    rec match {
//      case R("v" -> v) => v should be (1)
//    }
//
//    rec match {
//      case R("x" -> v) => v should be ("foo")
//    }
//
//    rec match {
//      case R("foo" -> v) => ???
//    }
//
//  }
