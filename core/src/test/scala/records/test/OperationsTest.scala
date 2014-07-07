package records.test

import org.scalatest._
import records.Operations._
import records.R

class OperationsTests extends FlatSpec with Matchers {

  
  case class LHS(id: Int, name: String)
  case class RHS(id: Int, phone: String)

  "A Record" should "be an output of the joined relation" in {
    
    val r = LHS(1,"foo") join RHS(1,"5555555")    

    r.name should be ("foo")
    r.phone should be ("5555555")
  }

}
