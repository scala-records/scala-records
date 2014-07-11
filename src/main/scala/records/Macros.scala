package records

import Compat210._

import scala.annotation.StaticAnnotation

object Macros {
  // Import macros only here, otherwise we collide with Compat210.whitebox
  import scala.reflect.macros._
  import whitebox.Context

  class RecordMacros[C <: Context](val c: C) {
    import c.universe._

    /** Create a generalized Record
      *
      * @param schema List of (field name, field type) tuples
      * @param ancestors Traits that are mixed into the resulting R
      *    (e.g. Serializable). Make sure the idents are fully
      *    qualified.
      * @param fields Additional members/fields of the resulting R
      *    (recommended for private data fields)
      * @param dataImpl Implementation of the [[__data]] method.
      *    Should use the parameter [[fieldName]] of type String and
      *    return a value of a corresponding type.
      */
    def record(schema: Seq[(String, Type)])(
      ancestors: Ident*)(fields: Tree*)(dataImpl: Tree) = {
      def fieldTree(i: Int, name: String, tpe: Type): Tree =
        q"""
          def ${newTermName(name).encodedName.toTermName}: $tpe =
            macro _root_.records.Macros.selectField_impl[$tpe]"""

      val macroFields =
        schema.zipWithIndex.map { case ((n, s), i) => fieldTree(i, n, s) }

      val resultTree = if (CompatInfo.isScala210) {
        q"""
        import scala.language.experimental.macros
        class Workaround extends _root_.records.R with ..$ancestors {
          ..$fields
          override def __data[T : _root_.scala.reflect.ClassTag](fieldName: String): T = $dataImpl
          ..$macroFields
        }
        new Workaround()
        """
      } else {
        q"""
        import scala.language.experimental.macros
        new _root_.records.R with ..$ancestors {
          ..$fields
          override def __data[T : _root_.scala.reflect.ClassTag](fieldName: String): T = $dataImpl
          ..$macroFields
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

      record(schema)()(q"private val _data = $data")(q"_data(fieldName).asInstanceOf[T]")
    }

    /** Generate a specialized data access on a record */
    def accessData(receiver: Tree, fieldName: String, tpe: Type): Tree = {
      import definitions._

      tpe match {
        case BooleanTpe =>
          q"$receiver.__dataBoolean($fieldName)"
        case ByteTpe =>
          q"$receiver.__dataByte($fieldName)"
        case ShortTpe =>
          q"$receiver.__dataShort($fieldName)"
        case CharTpe =>
          q"$receiver.__dataChar($fieldName)"
        case IntTpe =>
          q"$receiver.__dataInt($fieldName)"
        case LongTpe =>
          q"$receiver.__dataLong($fieldName)"
        case FloatTpe =>
          q"$receiver.__dataFloat($fieldName)"
        case DoubleTpe =>
          q"$receiver.__dataDouble($fieldName)"
        case _ =>
          q"$receiver.__dataObj[$tpe]($fieldName)"
      }
    }

    private def checkDuplicate(schema: Seq[(String, c.Type)]): Unit = {
      val duplicateFields = schema.groupBy(_._1).filter(_._2.size > 1)
      if (duplicateFields.nonEmpty) {
        val fields = duplicateFields.keys.toList.sorted
        if (fields.size == 1)
          c.abort(NoPosition, s"Field ${fields.head} is defined more than once.")
        else
          c.abort(NoPosition, s"Fields ${fields.mkString(", ")} are defined more than once.")
      }
    }

    object -> {
      def unapply(tree: Tree): Option[(Tree, Tree)] = tree match {
        // Scala 2.11.x
        case q"scala.this.Predef.ArrowAssoc[..${_}]($a).->[..${_}]($b)" => Some((a,b))
        // Scala 2.10.x
        case q"scala.this.Predef.any2ArrowAssoc[..${_}]($a).->[..${_}]($b)" => Some((a,b))
        case _ => None
      }
    }

  }

  def apply_impl(c: Context)(v: c.Expr[(String, Any)]*): c.Expr[R] =
    new RecordMacros[c.type](c).recordApply(v)

  def selectField_impl[T : c.WeakTypeTag](c: Context): c.Expr[T] = {
    import c.universe._

    val fieldName = newTermName(c.macroApplication.symbol.name.toString).decoded
    val tpe = implicitly[c.WeakTypeTag[T]].tpe

    val applyTree =
      new RecordMacros[c.type](c).accessData(c.prefix.tree, fieldName, tpe)

    c.Expr[T](applyTree)
  }

}
