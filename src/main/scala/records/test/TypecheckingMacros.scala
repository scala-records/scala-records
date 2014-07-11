package records
package test

import Compat210._
import scala.reflect.macros.TypecheckException

object TypecheckingMacros {
  import scala.reflect.macros
  import whitebox.Context

  private class Macro[C <: Context](val c: C) extends Internal210 {
    def typecheck(what: c.Expr[String],
                  expected: Option[String]): c.Expr[Unit] = {

      import c.universe._

      val Literal(Constant(toCompile)) = what.tree
      try {
        c.typecheck(c.parse(s"{ $toCompile }"))
        c.abort(c.enclosingPosition, "Expected type error, type checked successfully.")
      } catch {
        case e: TypecheckException =>
          val errMsg = e.getMessage
          expected foreach { msg0 =>
            if (errMsg != msg0)
              c.abort(c.enclosingPosition,
                      s"Type error messages mismatch.\nExpected: $msg0\nFound: $errMsg")
          }
      }
      c.Expr(q"()")
    }
  }


  def typed(c: Context)(what: c.Expr[String]): c.Expr[Unit] = {
    import c.universe._
    new Macro[c.type](c).typecheck(what, None)
  }

  def typedWithMsg(c: Context)(what: c.Expr[String], msg: c.Expr[String]): c.Expr[Unit] = {
    import c.universe._
    val Literal(Constant(v0: String)) = msg.tree
    new Macro[c.type](c).typecheck(what, Some(v0))
  }
}
