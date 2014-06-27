package records


import scala.reflect.macros.whitebox._
//import common.Rec

object Macros {
  def makeInstance(data: Product) = macro makeInstance_impl

  def makeInstance_impl(c: Context)(data: c.Expr[Product]) = 

    c.universe.reify[Any] {
      import scala.language.experimental.macros
    class Workaround {
      val data0 = data.splice
      def v: Any = macro Macros.selectField_impl
    }
    new Workaround {}
  }

  def selectField_impl(c: Context) = {
    import c.universe._

    val owner = c.prefix

    val res = Select(Select(c.prefix.tree, "data0"), "_1")
    showCode(res)
    c.Expr(res)
  }


}
