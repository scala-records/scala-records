package records

import Compat210._

/** Macros common to different sub-modules */
object CommonMacros {
  // Import macros only here, otherwise we collide with Compat210.whitebox
  import scala.reflect.macros._
  import whitebox.Context

  trait Common[C <: Context] extends Internal210 {
    val c: C
    import c.universe._

    type Schema = Seq[(String, Type)]

    /** Determine the fields of a record */
    def recordFields(recType: Type): Schema = {
      val base = recType.baseType(typeOf[Rec[Any]].typeSymbol)
      base match {
        case TypeRef(_, _, List(RefinedType(_, scope))) =>
          val fields =
            for (mem <- scope if mem.isMethod)
              yield (mem.name.encoded, mem.asMethod.returnType)
          fields.toSeq
        case _ =>
          Nil
      }
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

    /** Extractor for the tree of a Tuple2 */
    object Tuple2 {
      def unapply(tree: Tree): Option[(Tree, Tree)] = tree match {
        case q"($a, $b)" => Some((a, b))
        // Scala 2.11
        case q"scala.this.Tuple2.apply[..${ _ }]($a, $b)" => Some((a, b))
        // Scala 2.12
        case q"scala.Tuple2.apply[..${ _ }]($a, $b)" => Some((a, b))
        case _ => None
      }
    }

    /** Extractor for the tree of a Tuple2 in the form x -> y */
    object -> {
      def unapply(tree: Tree): Option[(Tree, Tree)] = tree match {
        // Scala 2.12.x
        case q"scala.Predef.ArrowAssoc[..${ _ }]($a).->[..${ _ }]($b)" => Some((a, b))
        // Scala 2.11.x
        case q"scala.this.Predef.ArrowAssoc[..${ _ }]($a).->[..${ _ }]($b)" => Some((a, b))
        // Scala 2.10.x
        case q"scala.this.Predef.any2ArrowAssoc[..${ _ }]($a).->[..${ _ }]($b)" => Some((a, b))
        case _ => None
      }
    }
  }
}
