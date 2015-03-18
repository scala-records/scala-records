package records

import Compat210._

import scala.annotation.StaticAnnotation

/**
 * Macros for pattern matching on Records.
 *
 * Not for public consumption
 */
object UnapplyMacros {
  // Import macros only here, otherwise we collide with Compat210.whitebox
  import scala.reflect.macros._
  import whitebox.Context

  class UnapplyMacros[C <: Context](val c: C) extends CommonMacros.Common[C] {
    import c.universe._

    sealed trait FieldPattern { def name: String }
    final case class TuplePattern(name: String) extends FieldPattern
    final case class BindPattern(name: String) extends FieldPattern

    def recordUnapply(scrutinee: c.Expr[Rec[Any]]): c.Expr[Any] = {
      if (CompatInfo.isScala210) {
        c.abort(c.enclosingPosition,
          "Record matching is not supported on 2.10.x")
      }

      val subPats = c.internal.subpatterns(scrutinee.tree).getOrElse {
        c.abort(c.enclosingPosition,
          "Rec.unapply only works in pattern matching mode")
      }

      val fieldPats: List[FieldPattern] = subPats.map {
        case pq"${ name: TermName } @ ${ _ }" => BindPattern(name.decoded)
        case pq"(${ s: String }, ${ _ })"     => TuplePattern(s)
        case other =>
          c.abort(other.pos, "Record field matcher must be a variable binding")
      }

      val body = {
        if (fieldPats.isEmpty) {
          q"true"
        } else {
          def fieldType(field: String): Option[Type] = {
            recordFields(scrutinee.tree.tpe).collectFirst {
              case (name, tpe) if name == field => tpe
            }
          }

          def fieldAccessor(field: String): Tree =
            accessData(q"rec", field, fieldType(field).getOrElse(typeOf[Any]))

          val guards = for {
            pat <- fieldPats
          } yield q"rec.__dataExists(${pat.name})"
          val guard = guards.reduce { (l, r) => q"$l && $r" }

          val accessors = fieldPats.map {
            case BindPattern(f)  => fieldAccessor(f)
            case TuplePattern(f) => q"($f, ${fieldAccessor(f)})"
          }

          q"""
            if ($guard) _root_.scala.Some((..$accessors))
            else _root_.scala.None
          """
        }
      }

      val expansion = q"""
        new {
          def unapply(rec: _root_.records.Rec[Any]) = $body
        }.unapply($scrutinee)
      """

      c.Expr[Any](expansion)
    }

  }

  def unapply_impl(c: Context)(scrutinee: c.Expr[Rec[Any]]): c.Expr[Any] =
    new UnapplyMacros[c.type](c).recordUnapply(scrutinee)

}
