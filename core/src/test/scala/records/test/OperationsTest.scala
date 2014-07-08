package records.test

import org.scalatest._
import records.Operations._
import records.R
import scala.language.reflectiveCalls

class OperationsTests extends FlatSpec with Matchers {

  
  case class LHS(id: Int, name: String)
  case class RHS(id: Int, phone: String)

  "A Record" should "be an output of the joined relation" in {
    
    val r = LHS(1,"foo") join RHS(1,"5555555")    

    r.name should be ("foo")
    r.phone should be ("5555555")
  }

  "A Record" should "should be joined out of combination of case classes and records" in {
    
    val rs1 = R("id" -> 1,"name" -> "foo") join RHS(1,"5555555")
    val rs2 = R("id" -> 1,"name" -> "foo") join R("id" -> 1,"phone" -> "5555555")
    val rs3 = LHS(1,"foo") join R("id" -> 1,"phone" -> "5555555")

    rs1.name should be ("foo")
    rs1.phone should be ("5555555")
    rs2.name should be ("foo")
    rs2.phone should be ("5555555")
    rs3.name should be ("foo")
    rs3.phone should be ("5555555")
  }
}
