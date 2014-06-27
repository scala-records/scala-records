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

  it should "allow to ascribe a result type" in {

    def query: R { def v: Any } =
      makeInstance(myDummyData)

    query.v should be (1)
  }

}
