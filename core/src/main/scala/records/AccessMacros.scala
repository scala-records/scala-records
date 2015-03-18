package records

import Compat210._

import scala.annotation.StaticAnnotation

/**
 * Macros to access record fields.
 *
 * Not for public consumption.
 */
object AccessMacros {
  // Import macros only here, otherwise we collide with Compat210.whitebox
  import scala.reflect.macros._
  import whitebox.Context

  class AccessImpls[C <: Context](val c: C) extends CommonMacros.Common[C] {
    import c.universe._

    /**
     * Prepare a record for access by exposing its fields as a structural type
     * with macro members.
     */
    def accessRecord[Fields: c.WeakTypeTag](rec: c.Expr[Rec[Fields]]): c.Expr[Fields] = {
      val schema = recordFields(rec.tree.tpe)

      val macroFields = schema.map((genRecordField _).tupled)

      val body = q"""
        import scala.language.experimental.macros
        new {
          private[this] val __rec = $rec
          ..$macroFields
        }
      """

      c.Expr[Fields](body)
    }

    def selectField[T: c.WeakTypeTag] = {
      import c.universe._

      val encodedName = c.macroApplication.symbol.name.toString
      val fieldName = newTermName(encodedName).decoded
      val tpe = c.macroApplication.symbol.asMethod.returnType

      val rec = c.prefix.tree match {
        case q"""
          import scala.language.experimental.macros
          new { private[this] val __rec = $tree; ..${ _ } }
        """ => tree
      }

      val applyTree = accessData(rec, fieldName, tpe)

      c.Expr[T](applyTree)
    }

    /**
     * Create a tree for a 'def' of a record field.
     * If a type of the field is yet another record, it will be a class type of the $\u200Banon
     * class (created as part of [[genRecord]]), rather than a pure RefinedType. Therefore
     * we have to recreate it and provide an appropriate type to the macro call.
     */
    def genRecordField(name: String, tpe: Type): Tree = {
      q"""
        def ${newTermName(name).encodedName.toTermName}: $tpe =
          macro _root_.records.AccessMacros.selectField_impl[$tpe]
      """
    }

  }

  def accessRecord_impl[Fields: c.WeakTypeTag](c: Context)(
    rec: c.Expr[Rec[Fields]]): c.Expr[Fields] =
    new AccessImpls[c.type](c).accessRecord(rec)

  def selectField_impl[T: c.WeakTypeTag](c: Context): c.Expr[T] =
    new AccessImpls[c.type](c).selectField

}
