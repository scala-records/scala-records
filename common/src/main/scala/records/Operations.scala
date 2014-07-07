package records

import RecordConversions._
import scala.language.experimental.macros
import Compat210._
import scala.annotation.StaticAnnotation

object Operations {
  // import macros only here, otherwise we collide with Compat210.whitebox
  import scala.reflect.macros._
  import whitebox.Context

  implicit class JoinR[LHS](val lhs: LHS) {
    import RecordConversions._
    def join[RHS](rhs: RHS): Any = macro implementations.join_impl[LHS, RHS]
  }
 
  object implementations {
  	import scala.language.existentials
  	def join_impl[LHS: c.WeakTypeTag, RHS: c.WeakTypeTag](c: Context)(rhs: c.Expr[RHS]) = {
      import c.universe._
      val rMacros = new Macros.RecordMacros[c.type](c)
      val conv = new ConversionMacros[c.type](c)

  		// check if it is a case class   		
  		val lhsType = weakTypeTag[LHS].tpe
      val lhsSym = lhsType.typeSymbol
      val rhsType = weakTypeTag[RHS].tpe
      val rhsSym = lhsType.typeSymbol

      if (!(lhsSym.asClass.isCaseClass || lhsType <:< typeOf[records.R]) &&
       !(rhsSym.asClass.isCaseClass || rhsType <:< typeOf[records.R])) {
        c.abort(NoPosition, "Only case classes and records can be joined.")
      }

      // get the lhs
      val Apply(TypeApply(Select(Select(_, _), TermName("JoinR")), List(TypeTree())), List(lhsTree)) = c.prefix.tree      
      val rhsTree = rhs.tree
      val (lhsVal, rhsVal) = (TermName(c.fresh("lhs$")), TermName(c.fresh("rhs$")))

      def fields(tpe: c.Type) = 
        if(tpe <:< typeOf[records.R]) conv.recordFields(tpe) else conv.caseClassFields(tpe)
      val (lhsFields, rhsFields) = (fields(lhsType), fields(rhsType))
      val commonFields = (lhsFields ++ rhsFields).distinct      

      def fieldConstructors(fields: List[(String, c.Type)], v: TermName): List[Tree] = 
        fields.map(_._1.toString).map(name => q"${Literal(Constant(name))} -> $v.${TermName(name)}")
      val constructorArgs = fieldConstructors(lhsFields, lhsVal) ++
       fieldConstructors(rhsFields.filter(!lhsFields.contains(_)), rhsVal)
  		
  		// make a record with a combination of fields
      q"""
        val $lhsVal = $lhsTree
        val $rhsVal = $rhsTree
        ${rMacros.record(commonFields)(c.Expr(q"""Map(..$constructorArgs)""")).tree}
      """  		  		
  	}
  }
}