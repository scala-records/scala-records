package ch.epfl

import scala.language.experimental.macros

import Compat210._

import scala.annotation.StaticAnnotation

object RecordConversions {
  // Import macros only here, otherwise we collide with Compat210.whitebox
  import scala.reflect.macros._
  import whitebox.Context

  def fromRecord_impl[From <: R : c.WeakTypeTag, To: c.WeakTypeTag](
      c: Context): c.Expr[To] =
      new ConversionMacros[c.type](c).createFromRecord[From, To]

  class ConversionMacros[C <: Context](val c: C) extends Internal210 {
    import c.universe._

    def createFromRecord[From <: R : WeakTypeTag, To : WeakTypeTag]: c.Expr[To] = {
      val fromType = weakTypeTag[From].tpe
      val toType = weakTypeTag[To].tpe
      val toSym = toType.typeSymbol

      if (!toSym.asClass.isCaseClass) {
        c.abort(NoPosition,
          "Currently, Records can only be converted to case classes.")
      }

      val fromFlds = recordFields(fromType).toMap
      val toFlds = caseClassFields(toType)
      val tmpTerm = newTermName(c.fresh("tmp$"))
      val args = for ((fname, ftpe) <- toFlds) yield {
        if (!fromFlds.contains(fname)) {
          c.abort(NoPosition,
            s"Source record is missing field $fname.")
        }

        val fType = fromFlds(fname)

        if (!(fType <:< ftpe)) {
          c.abort(NoPosition,
            s"Type of field $fname of source record ($fType) " +
            s"doesn't conform the expected type ($ftpe).")
        }

        // r is the source record
        q"${tmpTerm}.record.__data[$ftpe]($fname)"
      }

      val resTree = q"""
      val ${tmpTerm} = ${c.prefix.tree}
      new $toType(..$args)"""

      c.Expr(resTree)
    }

    def caseClassFields(ccType: Type): List[(String, c.Type)] = {
      val primCtor = ccType.members.collectFirst {
        case m if m.isMethod && m.asMethod.isPrimaryConstructor =>
          m.asMethod
      }.get

      if (primCtor.paramLists.size > 1)
        c.abort(NoPosition, "Target case class may only have a single parameter list.")

      (for (param <- primCtor.paramLists.head)
        yield (param.name.encoded, param.info)).toList
    }

    def recordFields(recType: Type): List[(String, c.Type)] = (for {
      mem <- recType.members
      if mem.isMacro && mem.isMethod
    } yield (mem.name.encoded, mem.asMethod.returnType)).toList

  }



}
