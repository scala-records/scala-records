package ch.epfl.test

import org.scalatest._
import ch.epfl.Operations._
import ch.epfl.R

class OperationsTests extends FlatSpec with Matchers {

  
  case class LHS(id: Int, name: String)
  case class RHS(id: Int, phone: String)

  "A Record" should "be an output of the joined relation" in {
    
    val r = LHS(1,"foo") join RHS(1,"5555555")    

    r.name should be ("foo")
    r.phone should be ("5555555")
  }

  "A Record" should "should be joined out of combination of case classes and records" in {
    
    val rs = List(
      R("id" -> 1,"name" -> "foo") join RHS(1,"5555555"),
      R("id" -> 1,"name" -> "foo") join R("id" -> 1,"phone" -> "5555555"),
      LHS(1,"foo") join R("id" -> 1,"phone" -> "5555555")
    )

    rs.foreach{ v=> 
      v.name should be ("foo")
      v.phone should be ("5555555")
    }
  }

}
