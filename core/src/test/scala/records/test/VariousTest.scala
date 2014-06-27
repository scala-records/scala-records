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

  it should "LOB lists" in { 
    import language.reflectiveCalls

    trait A
    val x = List(new A { val x = 1 }, new A { val x = 2 })

    x.head.x should be (1)
    x.last.x should be (2)

  }

  ignore should "allow different valued rows in ascribed lists" in {
    import language.reflectiveCalls

    val x = List[R { def v: Any }](makeInstance(myDummyData), makeInstance(List(2)))

    x.head.v should be (1)  // Runtime failure: NoSuchMethodException
    x.last.v should be (2)  // Runtime failure: NoSuchMethodException
  }

  ignore should "allow different valued rows in lists" in {
    val x = List(makeInstance(myDummyData), makeInstance(List(2)))

    //x.head.v should be (1)
    // value v is not a member of records.R{def row: List[Int]}
    //x.last.v should be (2)
    // value v is not a member of records.R{def row: List[Int]}
  }

  ignore should "allow to ascribe a result type" in {
    import language.reflectiveCalls

    def query: R { def v: Any } =
      makeInstance(myDummyData)

    query.v should be (1) // Runtime failure: NoSuchMethodException
  }

}
