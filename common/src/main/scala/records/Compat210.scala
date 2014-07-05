package records

object Compat210 {

  object whitebox {
    type Context = scala.reflect.macros.Context
  }

}
import Compat210._

trait Internal210 { self =>
  import scala.reflect.macros._
  import whitebox.Context

  val c: Context
  import c.universe._

  implicit class RichSymbol(val sym: Symbol) {
    def info = sym.typeSignature
  }

  implicit class RichMethodSymbol(val sym: MethodSymbol) {
    def paramLists = sym.paramss
  }
}

object CompatInfo {
  // This will shadow whitebox from Compat210 if we are on 2.11
  import scala.reflect.macros._

  def isScala210 = {
    classOf[whitebox.Context].getName == "scala.reflect.macros.Context"
  }

}
