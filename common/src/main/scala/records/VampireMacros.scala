package records

import scala.reflect.macros.whitebox._
import scala.annotation.StaticAnnotation

class body(index: Int, tpe: String) extends StaticAnnotation

object Macros {

  def rec(data: List[Any]): Any = macro rec_impl

  private def record(c: Context)(schema: Seq[(String, String)])(data: c.Expr[List[Any]]) = {
    import c.universe._
    def fieldTree(i: Int, name: String, tpe: String): Tree = q"""
       @body($i, $tpe) def ${TermName(name)}: ${TypeName(tpe)} =
         macro records.Macros.selectField_impl
    """

    val fields =
      schema.zipWithIndex.map { case ((n, s), i) => fieldTree(i, n, s) }

    q"""
      import scala.language.experimental.macros
      import records.body
      new R {
        protected val _data: List[Any] = ${data.tree}
        ..$fields
      }
    """
  }

  def apply_impl(c: Context)(v: c.Tree*): c.Tree = {
    import c.universe._
    val args = v.map {
      case Apply(
        TypeApply(
          Select(
            Apply(
              TypeApply(
                Select(Select(This(TypeName("scala")), TermName("Predef")), TermName("ArrowAssoc")),
                List(TypeTree())
              ),
              List(Literal(Constant(s)))
            ),
            TermName("$minus$greater")
          ),
            List(TypeTree())
        ),
        List(v)
      ) =>
       (s, v)
      case x =>
       c.error(NoPosition, "Rec must be used only with arguments StringLiteral -> value!")
       ("error", q"-1")
    }
    val schema = args.map(x => (x._1.toString, x._2.tpe.widen.toString))
    val data = q"List[Any](..${args.map(_._2)})"
    record(c)(schema)(c.Expr(data))
  }

  def rec_impl(c: Context)(data: c.Expr[List[Any]]) = {
    val schema: List[(String, String)] = List(("phone", "String"),("age", "Int"))
    record(c)(schema)(data)
  }

  def selectField_impl(c: Context) = {
    import c.universe._
    val args = c.macroApplication.symbol.annotations.filter(
      _.tpe <:< c.typeOf[body]
    ).head.scalaArgs

    val index :: Literal(Constant(tpe)) :: Nil = args

    q"${c.prefix.tree}.data($index).asInstanceOf[${TypeName(tpe.toString)}]"
  }

}
