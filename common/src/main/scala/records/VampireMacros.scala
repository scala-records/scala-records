package records

import scala.reflect.macros.whitebox._
import scala.annotation.StaticAnnotation

object Macros {

  private def record(c: Context)(schema: Seq[(String, c.Type)])(data: c.Expr[Map[String,Any]]) = {
    import c.universe._
    def fieldTree(i: Int, name: String, tpe: Type): Tree =
      q"def ${TermName(name)}: $tpe = macro records.Macros.selectField_impl"

    val fields =
      schema.zipWithIndex.map { case ((n, s), i) => fieldTree(i, n, s) }

    q"""
      import scala.language.experimental.macros
      new R {
        private val _data = ${data.tree}
        def data(fieldName: String): Any = _data(fieldName)
        ..$fields
      }
    """
  }

  def apply_impl(c: Context)(v: c.Tree*): c.Tree = {
    import c.universe._

    object -> {
      def unapply(tree: Tree): Option[(Tree, Tree)] = tree match {
        case Apply(
          TypeApply(
            Select(
              Apply(
                TypeApply(
                  Select(Select(This(TypeName("scala")), TermName("Predef")), TermName("ArrowAssoc")),
                  List(TypeTree())
                ),
                List(a)
              ),
              TermName("$minus$greater")
            ),
            List(TypeTree())
          ),
          List(b)
        ) => Some((a,b))
        case _ => None
      }
    }

    val schema = v.map {
      case Literal(Constant(s: String)) -> v => (s, v.tpe.widen)
      case x =>
        c.error(NoPosition, "Rec must be used only with arguments StringLiteral -> value!")
        ("error", NoType)
    }

    checkDuplicate(c)(schema)

    // We know `v` is of the right form, otherwise calculating schema would have failed
    val data = q"Map[String,Any](..$v)"

    record(c)(schema)(c.Expr(data))
  }

  def selectField_impl(c: Context) = {
    import c.universe._

    val fieldName = c.macroApplication.symbol.name.toString
    val tpe = c.macroApplication.symbol.asMethod.returnType

    q"${c.prefix.tree}.data($fieldName).asInstanceOf[$tpe]"
  }

  def checkDuplicate(c: Context)(schema: Seq[(String, c.Type)]): Unit = {
    import c.universe._

    val duplicateFields = schema.groupBy(_._1).filter(_._2.size > 1)
    if (duplicateFields.nonEmpty) {
      val fields = duplicateFields.keys
      if (fields.size == 1)
        c.error(NoPosition, s"Field ${fields.head} is defined more than once.")
      else
        c.error(NoPosition, s"Fields ${fields.mkString(", ")} are defined more than once.")
    }
  }

}
