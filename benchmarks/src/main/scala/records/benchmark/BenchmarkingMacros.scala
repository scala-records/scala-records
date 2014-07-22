package records
package benchmark

import scala.language.experimental.macros
import Compat210._

object BenchmarkingMacros {
  import scala.reflect.macros
  import whitebox.Context

  private class Macro[C <: Context](val c: C) extends Internal210 {

    def createSwitch(v: c.Tree, size: Int, expr: Int => c.Tree): c.Tree = {
      import c.universe._
      val cases = (1 to size) map (i =>
        cq"$i => iterations foreach {_ => ${expr(i)}}")
      q"""
        $v match {
          case ..$cases
        }
      """
    }

    def createHMap(i: Int): c.Tree = {
      import c.universe._
      val args = (1 to i) map (x => q"(${Literal(Constant(s"f$x"))} ->> $x)")
      val hMapTree = args.foldRight[Tree](q"HNil") { (arg, rhs) =>
        val tmpArg = TermName(c.freshName("arg$"))
        val tmpRhs = TermName(c.freshName("rhs$"))
        q"""
        val ${tmpArg} = ${arg}
        val ${tmpRhs} = {$rhs}
        $tmpArg :: $tmpRhs
        """
      }
      hMapTree
    }

    def createRec(i: Int): c.Tree = {
      import c.universe._
      val args = (1 to i) map (x => q"${Literal(Constant(s"f$x"))} -> $x")
      q"Rec(..$args)"
    }
  }

  def createRec(c: Context)(i: c.Expr[Int]): c.Expr[Any] = {
    import c.universe._
    val Literal(Constant(i1: Int)) = i.tree
    c.Expr(new Macro[c.type](c).createRec(i1))
  }

  def recSwitch(c: Context)(i: c.Expr[Int], size: c.Expr[Int]): c.Expr[Any] = {
    import c.universe._
    val Literal(Constant(size1: Int)) = size.tree
    val mac = new Macro[c.type](c)
    c.Expr(mac.createSwitch(i.tree, size1, i => mac.createRec(i)))
  }

  def accessRecSwitch(c: Context)(
    i: c.Expr[Int], size: c.Expr[Int], rec: c.Expr[Any]): c.Expr[Any] = {
    import c.universe._
    val Literal(Constant(size1: Int)) = size.tree
    val body = (i: Int) => q"${rec.tree}.${TermName("f" + i)}"
    c.Expr(new Macro[c.type](c).createSwitch(i.tree, size1, body))
  }

  def createHMap(c: Context)(i: c.Expr[Int]): c.Expr[Any] = {
    import c.universe._
    val Literal(Constant(i1: Int)) = i.tree
    c.Expr(new Macro[c.type](c).createHMap(i1))
  }

  def hMapSwitch(c: Context)(i: c.Expr[Int], size: c.Expr[Int]): c.Expr[Any] = {
    import c.universe._
    val Literal(Constant(size1: Int)) = size.tree
    val mac = new Macro[c.type](c)
    c.Expr(mac.createSwitch(i.tree, size1, i => mac.createHMap(i)))
  }

  def accessHMapSwitch(c: Context)(
    i: c.Expr[Int], size: c.Expr[Int], hmap: c.Expr[Any]): c.Expr[Any] = {
    import c.universe._
    val Literal(Constant(size1: Int)) = size.tree
    val body = (i: Int) => q"${hmap.tree}.get(${Literal(Constant("f" + i))})"
    c.Expr(new Macro[c.type](c).createSwitch(i.tree, size1, body))
  }

}
