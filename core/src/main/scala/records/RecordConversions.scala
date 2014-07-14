package records

import scala.language.experimental.macros

import Compat210._

import scala.annotation.StaticAnnotation

import Macros.RecordMacros

object RecordConversions {
  // Import macros only here, otherwise we collide with Compat210.whitebox
  import scala.reflect.macros._
  import whitebox.Context

  implicit def recordToCaseClass[From <: R, To <: Product]: From => To = macro recordToCaseClass_impl[From, To]

  def recordToCaseClass_impl[From <: R: c.WeakTypeTag, To <: Product: c.WeakTypeTag](c: Context): c.Expr[To] =
    new ConversionMacros[c.type](c).recordToCaseClass[From, To]

  def fromRecord_impl[From <: R: c.WeakTypeTag, To: c.WeakTypeTag](
    c: Context): c.Expr[To] = {
    import c.universe._
    val fromType = c.weakTypeTag[From].tpe
    val toType = c.weakTypeTag[To].tpe
    new ConversionMacros[c.type](c).createFromRecord[From, To](
      fromType, toType, q"${c.prefix.tree}.record")
  }

  class ConversionMacros[C <: Context](override val c: C)
    extends RecordMacros[C](c) with Internal210 {
    import c.universe._

    def recordToCaseClass[From <: R: c.WeakTypeTag, To <: Product: c.WeakTypeTag]: c.Expr[To] = {
      import c.universe._

      val validImplicit = c.openImplicits.collectFirst {
        case c.ImplicitCandidate(_, _, tp, _) =>
          tp
      }

      validImplicit match {
        case None =>
          c.abort(NoPosition, "The return type is not applicable to the record.")
        case Some(tp) =>
          val TypeRef(_, _, _ :: retType :: Nil) = tp.normalize
          val fromType = weakTypeTag[From].tpe
          val toType = retType.normalize
          val conversionTree = createFromRecord[From, To](fromType, toType, Ident("arg"))
          c.Expr(q"(((arg: ${TypeTree()}) => {$conversionTree}): $tp)")
      }
    }

    def createFromRecord[From <: R: WeakTypeTag, To: WeakTypeTag](
      fromType: Type, toType: Type, rec: Tree): c.Expr[To] = {
      val toSym = toType.typeSymbol

      if (!toSym.asClass.isCaseClass) {
        c.abort(NoPosition,
          s"Records can only be converted to case classes; $toType is not a case class.")
      }

      val fromFlds = recordFields(fromType).toMap
      val toFlds = caseClassFields(toType)
      val tmpTerm = newTermName(c.fresh("tmp$"))
      val missingFields = toFlds.map(_._1).filterNot(fromFlds.contains(_))
      if (missingFields.size > 0) {
        c.abort(NoPosition,
          s"Converting to $toType would require the source record to have the " +
            s"following additional fields: ${missingFields.mkString("[", ", ", "]")}.")
      }
      val args = for ((fname, ftpe) <- toFlds) yield {
        val fType = fromFlds(fname)

        if (!(fType <:< ftpe)) {
          c.abort(NoPosition,
            s"Type of field $fname of source record ($fType) " +
              s"doesn't conform the expected type ($ftpe).")
        }

        accessData(q"$tmpTerm", fname, ftpe)
      }

      val resTree = q"""
      val ${tmpTerm} = $rec
      new $toType(..$args)"""

      c.Expr(resTree)
    }

    def caseClassFields(ccType: Type) = {
      val primCtor = ccType.members.collectFirst {
        case m if m.isMethod && m.asMethod.isPrimaryConstructor =>
          m.asMethod
      }.get

      if (primCtor.paramLists.size > 1)
        c.abort(NoPosition, "Target case class may only have a single parameter list.")

      for (param <- primCtor.paramLists.head)
        yield (param.name.encoded, param.info)
    }

    def recordFields(recType: Type) = for {
      mem <- recType.members
      if mem.isMacro && mem.isMethod
    } yield (mem.name.encoded, mem.asMethod.returnType)

  }
}
