package records

object Compat210 {

  object whitebox {
    type Context = scala.reflect.macros.Context
  }

  object blackbox {
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
    def overrides: List[Symbol] = sym.allOverriddenSymbols
  }

  implicit class RichContext(val c: self.c.type) {
    object ImplicitCandidate {
      def unapply(x: (c.Type, c.Tree)): Option[(Type, Symbol, Type, Tree)] = {
        Some((NoType, NoSymbol, x._1, x._2))
      }
    }

    def typecheck(tree: c.universe.Tree): c.universe.Tree =
      c.typeCheck(tree)

    def internal: c.universe.type = c.universe
  }

  implicit class RichMethodSymbol(val sym: MethodSymbol) {
    def paramLists = sym.paramss
  }

  implicit class RichFlag(val flagValues: FlagValues) {
    // copied from Scalac source, since not available in 2.10
    final val SYNTHETIC = (1L << 21).asInstanceOf[FlagSet]
  }

  implicit class RichUniverse(val universe: c.universe.type) {
    def subpatterns(dummy: Tree): Option[List[Tree]] = None
  }

}

object CompatInfo {
  // This will shadow whitebox from Compat210 if we are on 2.11
  import scala.reflect.macros._

  def isScala210 = {
    classOf[whitebox.Context].getName == "scala.reflect.macros.Context"
  }

}
