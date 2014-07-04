package records

object Compat210 {

  object whitebox {
    type Context = scala.reflect.macros.Context
  }
}

object CompatInfo {
  import Compat210._

  def isScala210 = {
    // This will shadow whitebox from Compat210 if we are on 2.11
    import scala.reflect.macros._
    classOf[whitebox.Context].getName == "scala.reflect.macros.Context"
  }

}
