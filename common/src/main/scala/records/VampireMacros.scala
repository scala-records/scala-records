package records

import scala.reflect.macros.whitebox._
import scala.annotation.StaticAnnotation

class body(index: Int) extends StaticAnnotation

object Macros {

  private def record(c: Context)(schema: Seq[(String, c.Type)])(data: c.Expr[List[Any]]) = {
    import c.universe._
    def fieldTree(i: Int, name: String, tpe: Type): Tree = q"""
       @body($i) def ${TermName(name)}: $tpe =
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
    val args0 = v.map {
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

    val args = args0.sortBy(_._1.toString)
    val schema = args.map(x => (x._1.toString, x._2.tpe.widen))

    val duplicateFields = schema.groupBy(_._1).filter(_._2.size > 1)
    if (duplicateFields.nonEmpty) {
      val fields = duplicateFields.keys
      if (fields.size == 1)
        c.error(NoPosition, s"Field ${fields.head} is defined more than once.")
      else
        c.error(NoPosition, s"Fields ${fields.mkString(", ")} are defined more than once.")
    }

    val data = q"List[Any](..${args.map(_._2)})"
    record(c)(schema)(c.Expr(data))
  }

  def selectField_impl(c: Context) = {
    import c.universe._
    val args = c.macroApplication.symbol.annotations.filter(
      _.tpe <:< c.typeOf[body]
    ).head.scalaArgs

    val tpe = c.macroApplication.symbol.asMethod.returnType
    val List(index) = args

    q"${c.prefix.tree}.data($index).asInstanceOf[$tpe]"
  }

}
