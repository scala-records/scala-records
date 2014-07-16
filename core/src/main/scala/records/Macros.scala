package records

import Compat210._

import scala.annotation.StaticAnnotation

object Macros {
  // Import macros only here, otherwise we collide with Compat210.whitebox
  import scala.reflect.macros._
  import whitebox.Context

  class RecordMacros[C <: Context](val c: C) extends Internal210 {
    import c.universe._

    val rImplMods = Modifiers(Flag.OVERRIDE | Flag.SYNTHETIC)

    /**
     * Create a Record
     *
     * This creates a simple record that implements __data. As a
     * consequence it needs to box when used with primitive types.
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
      ancestors: Ident*)(fields: Tree*)(dataImpl: Tree): Tree = {

      val dataDef = q"""
        $rImplMods def __data[T : _root_.scala.reflect.ClassTag](
          fieldName: String): T = $dataImpl
      """

      genRecord(schema, ancestors, fields :+ dataDef)
    }

    /**
     * Create a specialized record
     *
     * By providing implementations for all or some primitive types,
     * boxing can be avoided.
     *
     * @param schema List of (field name, field type) tuples
     * @param ancestors Traits that are mixed into the resulting R
     *    (e.g. Serializable). Make sure the idents are fully
     *    qualified.
     * @param fields Additional members/fields of the resulting R
     *    (recommended for private data fields)
     * @param dataImpl Partial function giving the implementations of
     *    the __data* methods. If it is not defined for some of the
     *    __data* methods, {???} will be used instead. ObjectTpe is
     *    passed in for the generic version.
     *    Should use the parameter [[fieldName]] of type String and
     *    return a value of a corresponding type.
     */
    def spRecord(schema: Seq[(String, Type)])(
      ancestors: Ident*)(fields: Tree*)(
        dataImpl: PartialFunction[Type, Tree]): Tree = {

      import definitions._

      def impl(tpe: Type) =
        dataImpl.applyOrElse(tpe, (_: Type) => q"???")

      val dataDefs = q"""
        $rImplMods def __dataBoolean(fieldName: String): Boolean =
          ${impl(BooleanTpe)}
        $rImplMods def __dataByte(fieldName: String): Byte =
          ${impl(ByteTpe)}
        $rImplMods def __dataShort(fieldName: String): Short =
          ${impl(ShortTpe)}
        $rImplMods def __dataChar(fieldName: String): Char =
          ${impl(CharTpe)}
        $rImplMods def __dataInt(fieldName: String): Int =
          ${impl(IntTpe)}
        $rImplMods def __dataLong(fieldName: String): Long =
          ${impl(LongTpe)}
        $rImplMods def __dataFloat(fieldName: String): Float =
          ${impl(FloatTpe)}
        $rImplMods def __dataDouble(fieldName: String): Double =
          ${impl(DoubleTpe)}
        $rImplMods def __dataObj[T : _root_.scala.reflect.ClassTag](fieldName: String): T =
          ${impl(ObjectTpe)}
      """

      genRecord(schema, ancestors, fields :+ dataDefs)
    }

    /**
     * Generlalized record.
     * Implementation is totally left to the caller
     */
    def genRecord(schema: Seq[(String, Type)], ancestors: Seq[Ident],
                  impl: Seq[Tree]): Tree = {

      def enclClass(sym: Symbol): Symbol =
        if (sym.isClass) sym
        else enclClass(sym.owner)

      def isValidAccessor(methSym: MethodSymbol): Boolean =
        !methSym.isConstructor && methSym.isPublic && methSym.overrides.isEmpty

      /**
       * Create a tree for a 'def' of a field.
       * If a type of the field is yet another record, it will be a class type of the $anon
       * class (created as part of [[getRecord]]), rather than a pure RefinedType. Therefore
       * we have to recreate it and provide an appropriate type to the macro call.
       */
      def fieldTree(i: Int, name: String, tpe: Type): Tree = {
        val tpeOfField =
          if (tpe.baseType(rTpe.typeSymbol) != NoType) { // tpe.isInstanceOf[R] will return false.
            tpe.typeSymbol.typeSignature match {
              case cls @ ClassInfoType(parents, decls: Scope, tsym) =>
                // `cls` has in the scope declarations
                // for constructor and overriden __data. We want to remove them from
                // the scope of the RefinedType that we want to define.
                val decls1: Scope =
                  decls.filter {
                    case meth: MethodSymbol => isValidAccessor(meth)
                    case _                  => false
                  }.asInstanceOf[Scope]
                c.internal.refinedType(parents, enclClass(tpe.typeSymbol.owner), decls1, tpe.typeSymbol.pos)
              case _ =>
                tpe
            }
          } else tpe
        q"""
          def ${newTermName(name).encodedName.toTermName}: $tpeOfField =
            macro _root_.records.Macros.selectField_impl[$tpeOfField]"""
      }

      val macroFields =
        schema.zipWithIndex.map { case ((n, s), i) => fieldTree(i, n, s) }

      val resultTree = if (CompatInfo.isScala210) {
        q"""
        import scala.language.experimental.macros
        class Workaround extends _root_.records.Rec with ..$ancestors {
          ..$impl
          ..$macroFields
        }
        new Workaround()
        """
      } else {
        q"""
        import scala.language.experimental.macros
        new _root_.records.Rec with ..$ancestors {
          ..$impl
          ..$macroFields
        }
        """
      }

      resultTree
    }

    def recordApply(v: Seq[c.Expr[(String, Any)]]): c.Expr[Rec] = {
      val constantLiteralsMsg =
        "Records can only be constructed with constant keys (string literals)."
      val tuples = v.map(_.tree).map {
        case Literal(Constant(s: String)) -> v          => (s, v)
        case q"(${ Literal(Constant(s: String)) }, $v)" => (s, v)
        case q"($k, $v)" =>
          c.abort(NoPosition, constantLiteralsMsg)
        case _ -> _ =>
          c.abort(NoPosition, constantLiteralsMsg)
        case x =>
          c.abort(NoPosition, "Records can only be constructed with tuples (a, b) and arrows a -> b.")
      }

      val schema = tuples.map { case (s, v) => (s, v.tpe.widen) }

      checkDuplicate(schema)

      val args = tuples.map { case (s, v) => q"($s,$v)" }
      val data = q"Map[String,Any](..$args)"

      val resultTree =
        record(schema)()(
          q"private val _data = $data")(
            q"_data(fieldName).asInstanceOf[T]")

      c.Expr[Rec](resultTree)
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
        case q"scala.this.Predef.ArrowAssoc[..${ _ }]($a).->[..${ _ }]($b)" => Some((a, b))
        // Scala 2.10.x
        case q"scala.this.Predef.any2ArrowAssoc[..${ _ }]($a).->[..${ _ }]($b)" => Some((a, b))
        case _ => None
      }
    }

    private lazy val rTpe: Type = implicitly[WeakTypeTag[Rec]].tpe

  }

  def apply_impl(c: Context)(v: c.Expr[(String, Any)]*): c.Expr[Rec] =
    new RecordMacros[c.type](c).recordApply(v)

  def selectField_impl[T: c.WeakTypeTag](c: Context): c.Expr[T] = {
    import c.universe._

    val fieldName = newTermName(c.macroApplication.symbol.name.toString).decoded
    val tpe = implicitly[c.WeakTypeTag[T]].tpe

    val applyTree =
      new RecordMacros[c.type](c).accessData(c.prefix.tree, fieldName, tpe)

    c.Expr[T](applyTree)
  }

}
