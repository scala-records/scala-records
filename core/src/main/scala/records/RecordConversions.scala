package records

import scala.language.experimental.macros
import scala.language.implicitConversions

import Compat210._

import Macros.RecordMacros

object RecordConversions {
  // Import macros only here, otherwise we collide with Compat210.whitebox
  import scala.reflect.macros._
  import whitebox.Context

  object ConvertRecord {
    implicit def materializeConvert[From <: Rec[Any], To]: ConvertRecord[From, To] = macro materializeConvert_impl[From, To]
  }

  trait ConvertRecord[From, To] {
    def convert(r: From): To
  }

  implicit def convertRecord[From <: Rec[Any], To](x: From)(implicit ev: ConvertRecord[From, To]): To = ev.convert(x)

  def materializeConvert_impl[From <: Rec[Any]: c.WeakTypeTag, To: c.WeakTypeTag](c: Context): c.Expr[ConvertRecord[From, To]] = {
    import c.universe._
    new ConversionMacros[c.type](c).findConvertRecordCandidate[From, To]
  }

  def to_impl[From <: Rec[Any]: c.WeakTypeTag, To: c.WeakTypeTag](c: Context): c.Expr[To] = {
    import c.universe._
    val (fromTpe, toTpe) = (c.weakTypeTag[From].tpe, c.weakTypeTag[To].tpe)

    if (toTpe.typeSymbol.asType.isAbstractType)
      c.abort(NoPosition, s"Known limitation: Converting records requires an explicit type argument to `to` method representing the target case class")

    val typeClass =
      new ConversionMacros[c.type](c).convertRecordMaterializer(fromTpe, toTpe)

    c.Expr[To](q"""$typeClass.convert(${c.prefix.tree}.record)""")
  }

  class ConversionMacros[C <: Context](val c: C) extends CommonMacros.Common[C] {
    import c.universe._

    def findConvertRecordCandidate[From <: Rec[Any]: c.WeakTypeTag, To: c.WeakTypeTag]: c.Expr[ConvertRecord[From, To]] = {
      val allImplicitCandidates = c.openImplicits
        .map { case c.ImplicitCandidate(_, _, tp, tree) => tp.normalize }

      val implicitCandidates = allImplicitCandidates.collect {
        case TypeRef(_, _, _ :: toType :: Nil) if isCaseClass(toType) => toType
      }

      implicitCandidates match {
        case candidate :: Nil =>
          c.Expr(new ConversionMacros[c.type](c).convertRecordMaterializer(weakTypeTag[From].tpe, candidate))
        case l =>
          c.abort(NoPosition, s"There are ${l.size} implicit candidates found. There should be only 1.")
      }
    }

    def convertRecordMaterializer(fromType: Type, toType: Type, path: List[String] = Nil): Tree =
      convertRecordMaterializer(fromType, toType, toType, path)

    def convertRecordMaterializer(
      fromType:     Type,
      toType:       Type,
      originalType: Type,
      path:         List[String]): Tree = {

      def prefix(suffix: String) =
        if (path == Nil) "" else path.mkString("", ".", suffix)

      // check if the expected type is a case class
      if (!isCaseClass(toType)) {
        c.abort(NoPosition, s"Records can only be converted to case classes;" +
          s" $toType is not a case class.")
      }

      // check if the record has all the fields of the expected class
      val fromFlds = recordFields(fromType).toMap
      val toFlds = caseClassFields(toType)
      val missingFlds = toFlds.filterNot(x => fromFlds.contains(x._1))
      if (missingFlds.size > 0) {
        val fldsString = missingFlds.map(x => prefix(".") + x._1 + ": " + x._2).mkString("[", ", ", "]")
        c.abort(NoPosition, s"Converting to ${originalType} would require the " +
          s"source record to have the following additional fields: ${fldsString}.")
      }

      // do the conversion
      val tmpTerm = newTermName(c.fresh("tmp$"))
      val args = for ((toFldName, toFldTpe) <- toFlds) yield {
        val fromTpe = fromFlds(toFldName)
        if (fromTpe <:< typeOf[Rec[Any]] && isCaseClass(toFldTpe)) {
          // convert the nested record recursively
          val materializer =
            convertRecordMaterializer(fromTpe, toFldTpe, originalType, path :+ toFldName)
          q"$materializer.convert(${accessData(q"$tmpTerm", toFldName, fromTpe)})"
        } else if (fromTpe <:< toFldTpe) {
          // convert other types
          accessData(q"$tmpTerm", toFldName, fromTpe)
        } else {
          c.abort(NoPosition, s"Type of field ${prefix(".") + toFldName}: $fromTpe" +
            s" of source record doesn't conform the expected type ($toFldTpe).")
        }
      }

      q"""
        new _root_.records.RecordConversions.ConvertRecord[$fromType, $toType] {
          def convert(r: $fromType) = {
            val ${tmpTerm} = r
            new $toType(..$args)
          }
        }
      """
    }

    def caseClassFields(ccType: Type) = {
      val primCtor = ccType.members.collectFirst {
        case m: MethodSymbol if m.isPrimaryConstructor => m
      }.get

      if (primCtor.paramLists.size > 1)
        c.abort(NoPosition, "Target case class may only have a single parameter list.")

      val MethodType(params, _) = primCtor.typeSignatureIn(ccType)
      params map (param => (param.name.encoded, param.typeSignature))
    }

    private def isCaseClass(tpe: Type) =
      tpe.typeSymbol.isClass && tpe.typeSymbol.asClass.isCaseClass
  }
}
