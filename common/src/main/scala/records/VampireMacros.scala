package ch.epfl

import Compat210._

import scala.annotation.StaticAnnotation

object Macros {
  // Import macros only here, otherwise we collide with Compat210.whitebox
  import scala.reflect.macros._
  import whitebox.Context

  class RecordMacros[C <: Context](val c: C) {
    import Compat210._
    import c.universe._

    private def record(schema: Seq[(String, Type)])(data: c.Expr[Map[String,Any]]) = {
      def fieldTree(i: Int, name: String, tpe: Type): Tree =
        q"def ${newTermName(name)}: $tpe = macro ch.epfl.Macros.selectField_impl[$tpe]"

      val fields =
        schema.zipWithIndex.map { case ((n, s), i) => fieldTree(i, n, s) }

      val resultTree = if (CompatInfo.isScala210) {
        q"""
        import scala.language.experimental.macros
        class Workaround extends ch.epfl.R {
          private val _data = ${data.tree}
          def __data[T](fieldName: String): T = _data(fieldName).asInstanceOf[T]
          ..$fields
        }
        new Workaround()
        """
      } else {
        q"""
        import scala.language.experimental.macros
        new ch.epfl.R {
          private val _data = ${data.tree}
          def __data[T](fieldName: String): T = _data(fieldName).asInstanceOf[T]
          ..$fields
        }
        """
      }

      c.Expr(resultTree)
    }

    def recordApply(v: Seq[c.Expr[(String, Any)]]): c.Expr[R] = {
      val constantLiteralsMsg =
        "Records can only be constructed with constant keys (string literals)."
      val tuples = v.map(_.tree).map {
        case Literal(Constant(s: String)) -> v => (s, v)
        case q"(${Literal(Constant(s: String))}, $v)" => (s,v)
        case q"($k, $v)"  =>
          c.abort(NoPosition, constantLiteralsMsg)
        case _ -> _ =>
          c.abort(NoPosition, constantLiteralsMsg)
        case x =>
          c.abort(NoPosition, "Records can only be constructed with tuples (a, b) and arrows a -> b.")
      }

      val schema = tuples.map { case (s,v) => (s, v.tpe.widen) }

      checkDuplicate(schema)

      val args = tuples.map { case (s,v) => q"($s,$v)" }
      val data = q"Map[String,Any](..$args)"

      record(schema)(c.Expr(data))
    }

    private def checkDuplicate(schema: Seq[(String, c.Type)]): Unit = {
      val duplicateFields = schema.groupBy(_._1).filter(_._2.size > 1)
      if (duplicateFields.nonEmpty) {
        val fields = duplicateFields.keys
        if (fields.size == 1)
          c.abort(NoPosition, s"Field ${fields.head} is defined more than once.")
        else
          c.abort(NoPosition, s"Fields ${fields.mkString(", ")} are defined more than once.")
      }
    }

    object -> {
      val Scala = newTypeName("scala")
      val Predef = newTermName("Predef")
      val ArrowAssoc = newTermName("ArrowAssoc")
      val Any2ArrowAssoc = newTermName("any2ArrowAssoc")
      val `$minus$greater` = newTermName("$minus$greater")

      def unapply(tree: Tree): Option[(Tree, Tree)] = tree match {
        // Scala 2.11.x
        case Apply(
          TypeApply(
            Select(
              Apply(
                TypeApply(
                  Select(Select(This(Scala), Predef), ArrowAssoc),
                  List(TypeTree())
                ),
                List(a)
              ),
              `$minus$greater`
            ),
            List(TypeTree())
          ),
          List(b)
        ) => Some((a,b))
        // Scala 2.10.x
        case Apply(
          TypeApply(
            Select(
              Apply(
                TypeApply(
                  Select(Select(This(Scala), Predef), Any2ArrowAssoc),
                  List(TypeTree())
                ),
                List(a)
              ),
              `$minus$greater`
            ),
            List(TypeTree())
          ),
          List(b)
        ) => Some((a,b))
        case _ => None
      }
    }

  }

  def apply_impl(c: Context)(v: c.Expr[(String, Any)]*): c.Expr[R] =
    new RecordMacros[c.type](c).recordApply(v)

  def selectField_impl[T : c.WeakTypeTag](c: Context): c.Expr[T] = {
    import c.universe._

    val fieldName = c.macroApplication.symbol.name.toString
    val tpe = implicitly[c.WeakTypeTag[T]].tpe

    c.Expr[T](q"${c.prefix.tree}.__data[$tpe]($fieldName)")
  }

}
