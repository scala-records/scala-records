package records

import Compat210._

import scala.annotation.StaticAnnotation

object Macros {
  // Import macros only here, otherwise we collide with Compat210.whitebox
  import scala.reflect.macros._
  import whitebox.Context

  class RecordMacros[C <: Context](val c: C) extends CommonMacros.Common[C] {
    import c.universe._

    protected val rImplMods = Modifiers(Flag.OVERRIDE | Flag.SYNTHETIC)
    protected val synthMod = Modifiers(Flag.SYNTHETIC)

    /** Set of types for which the `_\u200B_data*` members are specialized */
    val specializedTypes: Set[Type] = {
      import definitions._
      Set(BooleanTpe, ByteTpe, CharTpe, ShortTpe, IntTpe, LongTpe, FloatTpe, DoubleTpe)
    }

    /**
     * Create a Record
     *
     * This creates a simple record that implements `_\u200B_data`. As a
     * consequence it needs to box when used with primitive types.
     *
     * @param schema List of (field name, field type) tuples
     * @param ancestors Traits that are mixed into the resulting [[Rec]]
     *    (e.g. Serializable). Make sure the idents are fully qualified.
     * @param fields Additional members/fields of the resulting [[Rec]]
     *    (recommended for private data fields)
     * @param dataImpl Implementation of the `_\u200B_data` method.
     *    Should use the parameter `fieldName` of type String and the type
     *    parameter `T` and return a value of type `T`
     *    return a value of a corresponding type.
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
     * @param ancestors Traits that are mixed into the resulting [[Rec]]
     *    (e.g. Serializable). Make sure the idents are fully qualified.
     * @param fields Additional members/fields of the resulting [[Rec]]
     *    (recommended for private data fields)
     * @param objectDataImpl Implementation of the `_\u200B_dataObj` method. Should
     *    use the parameter `fieldName` of type String and the type parameter
     *    `T` and return a value of type `T`
     * @param dataImpl Partial function giving the implementations of
     *    the `_\u200B_data*` methods. If it is not defined for some of the
     *    `_\u200B_data*` methods, `???` will be used instead.
     *    Should use the parameter `fieldName` of type String and
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
     * Generalized record. Implementation is totally left to the caller.
     * @param schema List of (field name, field type) tuples
     * @param ancestors Traits that are mixed into the resulting [[Rec]]
     *    (e.g. Serializable). Make sure the idents are fully qualified.
     * @param impl However you want to implement the `_\u200B_data` interface.
     */
    def genRecord(schema: Schema, ancestors: Seq[Ident],
                  impl: Seq[Tree]): Tree = {

      val dataCountTree = q"$synthMod def __dataCount = ${schema.size}"

      val body = impl ++ Seq(
        genToString(schema),
        genHashCode(schema),
        genDataExists(schema),
        genDataAny(schema),
        genEquals(schema),
        dataCountTree)

      val structType = {
        val fields = for {
          (name, tpe) <- schema
        } yield {
          val encName = newTermName(name).encodedName.toTermName
          q"def $encName: $tpe"
        }
        tq"{ ..$fields }"
      }

      q"""
      new _root_.records.Rec[$structType] with ..$ancestors {
        ..$body
      }: _root_.records.Rec[$structType] with ..$ancestors
      """
    }

    /**
     * Generate the toString method of a record. The resulting toString
     *  method will generate strings of the form:
     *  {{{
     *  Rec { fieldName1 = fieldValue1, fieldName2 = fieldValue2, ... }
     *  }}}
     */
    def genToString(schema: Schema): Tree = {
      val elems = for ((fname, tpe) <- schema) yield {
        val fldVal = accessData(q"this", fname, tpe)
        q"""$fname + " = " + $fldVal.toString"""
      }

      val cont = elems.reduceLeftOption[Tree] {
        case (acc, e) => q"""$acc + ", " + $e"""
      }

      val str = cont.fold[Tree](q""" "Rec {}" """) { cont =>
        q""" "Rec { " + $cont + " }" """
      }

      q"override def toString(): String = $str"
    }

    /**
     * Generate the hashCode method of a record. The hasCode is an bitwise xor
     *  of the hashCodes of the field names (this one is calculated at compile
     *  time) and the hashCodes of the field values
     */
    def genHashCode(schema: Schema): Tree = {
      import scala.util.hashing.MurmurHash3._

      val sortedSchema = schema.sortBy(_._1)

      val recSeed = -972824572

      // Hash of all field names
      val nameHash = sortedSchema.foldLeft(recSeed) {
        case (hash, (name, _)) => mix(hash, name.hashCode)
      }

      // Hashes of fields
      val fieldHashes = sortedSchema.map {
        case (name, tpe) =>
          val data = accessData(q"this", name, tpe)
          q"$data.##"
      }

      val mm3 = q"_root_.scala.util.hashing.MurmurHash3"

      val hashBody = fieldHashes.foldLeft[Tree](q"$nameHash") {
        case (acc, hash) => q"$mm3.mix($acc, $hash)"
      }

      val elemCount = sortedSchema.size * 2

      q"""
      override def hashCode(): Int = $mm3.finalizeHash($hashBody, $elemCount)
      """
    }

    /** Generate `_\u200B_dataExists` member */
    def genDataExists(schema: Schema): Tree = {
      val lookupData = schema.map { case (name, _) => (name, q"true") }.toMap
      val lookupTree =
        genLookup(q"fieldName", lookupData, default = Some(q"false"))

      q"$synthMod def __dataExists(fieldName: String): Boolean = $lookupTree"
    }

    /** Generate `_\u200B_dataAny` member */
    def genDataAny(schema: Schema): Tree = {
      val lookupData = schema.map {
        case (name, tpe) =>
          (name, accessData(q"this", name, tpe))
      }.toMap
      val lookupTree = genLookup(q"fieldName", lookupData, mayCache = false)

      q"$synthMod def __dataAny(fieldName: String): Any = $lookupTree"
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
        case that: _root_.records.Rec[Any] if that.__dataCount == $thisCount =>
          ${tests.fold[Tree](q"true") { case (x, y) => q"$x && $y" }}
        case _ => false
      }
      """
    }

    /**
     *  Generate a lookup amongst the keys in `data` and map to the tree
     *  values. This is like an exhaustive pattern match on the strings, but may
     *  be implemented more efficiently.
     *  If default is None, it is assumed that `nameTree` evaluates to one of
     *  the keys of `data`. Otherwise the default tree is used if a key
     *  doesn't exist.
     *  If `mayCache` is true, the implementation might decide to store the
     *  evaluated trees somewhere (at runtime). Otherwise, the trees will be
     *  evaluated each time the resulting tree is evaluated.
     */
    def genLookup(nameTree: Tree, data: Map[String, Tree],
                  default: Option[Tree] = None, mayCache: Boolean = true): Tree = {

      val count = data.size + default.size

      if (count == 0) {
        q"???"
      } else if (count == 1) {
        // Shortcut for only one case
        data.values.headOption.orElse(default).get
      } else if (data.size == 1) {
        // No need doing switch. We have a normal and a default case
        val (keyStr, tree) = data.head
        q"""if ($nameTree == $keyStr) $tree else ${default.get}"""
      } else if (data.contains("")) {
        // Special case this, since the per-char distinction won't work
        val lookupRest = genLookup(nameTree, data - "", default, mayCache)
        q"""if ($nameTree == "") ${data("")} else $lookupRest"""
      } else {
        val keys = data.keys.toList
        val minSize = keys.map(_.length).min

        val optSplitIdx = {
          val optimality = for (i <- 0 until minSize)
            yield (i, keys.map(_.charAt(i)).distinct.size)
          optimality.maxBy(_._2)._1
        }

        val grouped = data.groupBy(_._1.charAt(optSplitIdx))

        val cases0 = grouped.map {
          case (c, innerData) =>
            val body = {
              if (innerData.size == 1 && default.isEmpty)
                // Only one key matches and no default. Done
                innerData.values.head
              else
                genTrivialLookup(nameTree, innerData, default)
            }

            cq"$c => $body"
        }

        val cases1 = cases0 ++ default.map(default => cq"_ => $default")

        val switchAnnot = tq"_root_.scala.annotation.switch"
        q"""
          ($nameTree.charAt($optSplitIdx): @$switchAnnot) match {
            case ..$cases1
          }
        """
      }
    }

    private def genTrivialLookup(nameTree: Tree, data: Map[String, Tree],
                                 default: Option[Tree]) = {

      val cases0 = data.map {
        case (name, res) => cq"$name => $res"
      }

      val cases1 = cases0 ++ default.map(default => cq"_ => $default")

      q"$nameTree match { case ..$cases1 }"
    }

    /**
     * Macro that implements [[Rec.applyDynamic]] and [[Rec.applyDynamicNamed]].
     * You probably won't need this.
     */
    def recordApply(v: Seq[c.Expr[(String, Any)]]): c.Expr[Rec[Any]] = {
      val constantLiteralsMsg =
        "Records can only be constructed with constant keys (string literals)."
      val noEmptyStrMsg =
        "Records may not have a field with an empty name"

      val tuples = v.map(_.tree).map {
        case Tuple2(Literal(Constant(s: String)), v) =>
          if (s == "") c.abort(NoPosition, noEmptyStrMsg)
          else (s, v)
        case Literal(Constant(s: String)) -> v =>
          if (s == "") c.abort(NoPosition, noEmptyStrMsg)
          else (s, v)
        case Tuple2(_, _) =>
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

      c.Expr[Rec[Any]](resultTree)
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
  }

  def apply_impl(c: Context)(method: c.Expr[String])(v: c.Expr[(String, Any)]*): c.Expr[Rec[Any]] = {
    import c.universe._
    method.tree match {
      case Literal(Constant(str: String)) if str == "apply" =>
        new RecordMacros[c.type](c).recordApply(v)
      case Literal(Constant(str: String)) =>
        val targetName = c.prefix.actualType.typeSymbol.fullName
        c.abort(NoPosition,
          s"value $str is not a member of $targetName")
      case _ =>
        val methodName = c.macroApplication.symbol.name
        c.abort(NoPosition,
          s"You may not invoke Rec.$methodName with a non-literal method name.")
    }
  }

}
