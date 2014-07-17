package records

import Compat210._

import scala.annotation.StaticAnnotation

object Macros {
  // Import macros only here, otherwise we collide with Compat210.whitebox
  import scala.reflect.macros._
  import whitebox.Context

  class RecordMacros[C <: Context](val c: C) extends Internal210 {
    import c.universe._

    type Schema = Seq[(String, Type)]

    val rImplMods = Modifiers(Flag.OVERRIDE | Flag.SYNTHETIC)
    val synthMod = Modifiers(Flag.SYNTHETIC)

    val specializedTypes = {
      import definitions._
      Set(BooleanTpe, ByteTpe, CharTpe, ShortTpe, IntTpe, LongTpe, FloatTpe, DoubleTpe)
    }

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
     *    Should use the parameter [[fieldName]] of type String and the type
     *    parameter [[T]] and return a value of type [[T]]
     */
    def record(schema: Schema)(ancestors: Ident*)(
      fields: Tree*)(dataImpl: Tree): Tree = {

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
     * @param objectDataImpl Implementation of the [[__dataObj]] method. Should
     *    use the parameter [[fieldName]] of type String and the type parameter
     *    [[T]] and return a value of type [[T]]
     * @param dataImpl Partial function giving the implementations of
     *    the __data* methods. If it is not defined for some of the
     *    __data* methods, {???} will be used instead.
     *    Should use the parameter [[fieldName]] of type String and
     *    return a value of a corresponding type.
     *    The partial function will be called exactly once with each value in
     *    [[specializedTypes]].
     */
    def specializedRecord(schema: Schema)(ancestors: Ident*)(fields: Tree*)(
      objectDataImpl: Tree)(dataImpl: PartialFunction[Type, Tree]): Tree = {

      import definitions._

      def impl(tpe: Type) = dataImpl.applyOrElse(tpe, (_: Type) => q"???")

      val specializedDefs = specializedTypes.map { t =>
        val name = t.typeSymbol.name
        val methodName = newTermName("__data" + name)
        q"$rImplMods def $methodName(fieldName: String): $t = ${impl(t)}"
      }

      val objectDef = q"""
        $rImplMods def __dataObj[T : _root_.scala.reflect.ClassTag](
          fieldName: String): T = $objectDataImpl
      """

      genRecord(schema, ancestors, fields ++ specializedDefs :+ objectDef)
    }

    /**
     * Generalized record.
     * Implementation is totally left to the caller
     */
    def genRecord(schema: Schema, ancestors: Seq[Ident],
                  impl: Seq[Tree]): Tree = {

      val macroFields = schema.map((genRecordField _).tupled)

      val dataCountTree = q"$synthMod def __dataCount = ${schema.size}"

      val body = impl ++ macroFields ++ Seq(
        genToString(schema),
        genHashCode(schema),
        genDataExists(schema),
        genDataAny(schema),
        genEquals(schema),
        dataCountTree)

      val resultTree = if (CompatInfo.isScala210) {
        q"""
        import scala.language.experimental.macros
        class Workaround extends _root_.records.Rec with ..$ancestors {
          ..$body
        }
        new Workaround()
        """
      } else {
        q"""
        import scala.language.experimental.macros
        new _root_.records.Rec with ..$ancestors {
          ..$body
        }
        """
      }

      resultTree
    }

    /**
     * Create a tree for a 'def' of a record field.
     * If a type of the field is yet another record, it will be a class type of the $anon
     * class (created as part of [[getRecord]]), rather than a pure RefinedType. Therefore
     * we have to recreate it and provide an appropriate type to the macro call.
     */
    def genRecordField(name: String, tpe: Type): Tree = {
      def enclClass(sym: Symbol): Symbol =
        if (sym.isClass) sym
        else enclClass(sym.owner)

      def isValidAccessor(methSym: MethodSymbol): Boolean =
        !methSym.isConstructor && methSym.isPublic && methSym.overrides.isEmpty

      val tpeOfField =
        if (tpe.baseType(rTpe.typeSymbol) != NoType) {
          // tpe.isInstanceOf[Rec] will return false.
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
              c.internal.refinedType(parents, enclClass(tpe.typeSymbol.owner),
                decls1, tpe.typeSymbol.pos)
            case _ =>
              tpe
          }
        } else tpe
      q"""
        def ${newTermName(name).encodedName.toTermName}: $tpeOfField =
          macro _root_.records.Macros.selectField_impl[$tpeOfField]
      """
    }

    /**
     * Generate the toString method of a record. The resulting toString
     *  method will generate strings of the form:
     *  Rec { fieldName1 = fieldValue1, fieldName2 = fieldValue2, ... }
     */
    def genToString(schema: Schema): Tree = {
      val elems = for ((fname, tpe) <- schema) yield {
        val fldVal = accessData(q"this", fname, tpe)
        q"""$fname + " = " + $fldVal.toString"""
      }

      val cont = elems.reduceLeftOption[Tree] {
        case (acc, e) => q"""$acc + ", " + $e"""
      }

      val str = cont.fold[Tree](q""""Rec {}"""") { cont =>
        q""""Rec { " + $cont + " }""""
      }

      q"override def toString(): String = $str"
    }

    /**
     * Generate the hashCode method of a record. The hasCode is an bitwise xor
     *  of the hashCodes of the field names (this one is calculated at compile
     *  time) and the hashCodes of the field values
     */
    def genHashCode(schema: Schema): Tree = {

      // Hash of all field names
      val nameHash = schema.foldLeft(0) {
        case (hash, (name, _)) =>
          hash ^ name.hashCode
      }

      // Hashes of fields
      val fieldHashes = schema.map {
        case (name, tpe) =>
          val data = accessData(q"this", name, tpe)
          q"$data.##"
      }

      val hashBody = fieldHashes.foldLeft[Tree](q"$nameHash") {
        case (acc, hash) => q"$acc ^ $hash"
      }

      q"override def hashCode(): Int = $hashBody"
    }

    /** Generate __dataExists member */
    def genDataExists(schema: Schema): Tree = {
      val lookupData = schema.map { case (name, _) => (name, q"true") }.toMap
      val lookupTree =
        genLookup(q"fieldName", lookupData, default = Some(q"false"))

      q"$synthMod def __dataExists(fieldName: String) = $lookupTree"
    }

    /** Generate __dataAny member */
    def genDataAny(schema: Schema): Tree = {
      val lookupData = schema.map {
        case (name, tpe) =>
          (name, accessData(q"this", name, tpe))
      }.toMap
      val lookupTree = genLookup(q"fieldName", lookupData, mayCache = false)

      q"$synthMod def __dataAny(fieldName: String) = $lookupTree"
    }

    /**
     * Generate the equals method for Records. Two records are equal iff:
     *  - They have the same number of fields
     *  - Their fields have the same names
     *  - Values of corresponding fields compare equal
     */
    def genEquals(schema: Schema): Tree = {
      val thisCount = schema.size

      val existence = schema.map { case (n, _) => q"that.__dataExists($n)" }
      val equality = schema.map {
        case (name, tpe) =>
          q"${accessData(q"this", name, tpe)} == that.__dataAny($name)"
      }

      val tests = existence ++ equality

      q"""
      override def equals(that: Any) = that match {
        case that: _root_.records.Rec if that.__dataCount == $thisCount =>
          ${tests.fold[Tree](q"true") { case (x, y) => q"$x && $y" }}
        case _ => false
      }
      """
    }

    /**
     * Generate a lookup amongst the keys in [[data]] and map to the tree
     *  values. This is like an exhaustive pattern match on the strings, but may
     *  be implemented more efficiently.
     *  If default is None, it is assumed that [[nameTree]] evaluates to one of
     *  the keys of [[data]]. Otherwise the default tree is used if a key
     *  doesn't exist.
     *  If [[mayCache]] is true, the implementation might decide to store the
     *  evaluated trees somewhere (at runtime). Otherwise, the trees will be
     *  evaluated each time the resulting tree is evaluated.
     */
    def genLookup(nameTree: Tree, data: Map[String, Tree],
                  default: Option[Tree] = None, mayCache: Boolean = true): Tree = {

      val cases0 = data.map {
        case (name, res) => cq"$name => $res"
      }

      val cases1 = cases0 ++ default.map(default => cq"_ => $default")

      q"$nameTree match { case ..$cases1 }"
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
