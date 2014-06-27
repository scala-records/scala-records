package records.test

import org.scalatest._

import records.Macros.makeInstance
import records.R

class VariousTests extends FlatSpec with Matchers {

  val myDummyData: List[Int] = List(1, 2)

  "A Record" should "allow to read the value directly" in {
    val record = makeInstance(myDummyData)

    record.v should be (1)
  }

  it should "allow to read the value in a closure" in {
    val record = makeInstance(myDummyData)

    val f = () => record.v

    f() should be (1)
  }

  it should "allow Rows as a result in a method type" in {

    def query = makeInstance(myDummyData)

    query.v should be (1)

  }

  it should "allow rows in generics" in {
    import language.existentials

    class Box[T](val x: T) {
      def get = x
    }

    val x = new Box(makeInstance(myDummyData))

    x.get.v should be (1)

  }

  it should "allow to fill lists" in {
    val x = List.fill(1)(makeInstance(myDummyData))

    x.head.v should be (1)
  }

  it should "LUB properly" in {
    val x = List(makeInstance(myDummyData), makeInstance(List(2)))

    x.head.v should be (1)
    x.last.v should be (2)
    val r = if (true) makeInstance(myDummyData) else makeInstance(List(2))
    r.v should be (1)
  }

  // this is defined by the user. Could also be a case class!
  trait VHolder { def v: Any }

  // it would be good if a generic macro could take care of this for all return types!
  import scala.language.implicitConversions
  implicit def rowToEntity[T <: R](x: T): VHolder = {
    new VHolder { def v = x.row(0) }
  }

  it should "allow different valued rows in ascribed lists" in {
    // This would be too cool. But lets ask the user to provide a case class for each explicit case.
    // val x = List[R { def v: Any }](makeInstance(myDummyData), makeInstance(List(2)))
    val x = List[VHolder](makeInstance(myDummyData), makeInstance(List(2)))
    x.head.v should be (1)
    x.last.v should be (2)
  }

  it should "allow to ascribe a result type" in {

    def query: VHolder =
      makeInstance(myDummyData)

    query.v should be (1)
  }

}
