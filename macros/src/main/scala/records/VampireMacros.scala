package records

import scala.reflect.macros.whitebox._

object Macros {
  def makeInstance(data: List[Any]) = macro makeInstance_impl

  def makeInstance_impl(c: Context)(data: c.Expr[List[Any]]) = {
    c.universe.reify[Any] {
      import scala.language.experimental.macros
      new R {
        protected val _row = data.splice
        def v: Any = macro Macros.selectField_impl
      }
    }
  }

  def selectField_impl(c: Context) = {
    import c.universe._

    val owner = c.prefix
    val res = Apply(Select(Select(c.prefix.tree, "row"), "apply"), Literal(Constant(0)) :: Nil)

    c.Expr(res)
  }

}
