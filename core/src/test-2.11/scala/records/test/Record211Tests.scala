package records.test

import org.scalatest._

import records.R

class Record211Tests extends FlatSpec with Matchers {

  "A Record" should "not depend on declared field order" in {

    val people = List(
      R("age" -> 1, "name" -> "Michael"),
      R("name" -> "Ahir", "age" -> 23))

    people.head.name should be ("Michael")
    people.last.name should be ("Ahir")

  }

}
