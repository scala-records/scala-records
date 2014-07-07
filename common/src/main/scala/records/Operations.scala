package records

import RecordConversions._
import scala.language.experimental.macros
import Compat210._
import scala.annotation.StaticAnnotation

object Operations {
  // import macros only here, otherwise we collide with Compat210.whitebox
  import scala.reflect.macros._
  import whitebox.Context

  implicit class JoinR[LHS <: Product](val lhs: LHS) {
    import RecordConversions._
    def join[RHS <: Product](rhs: RHS): Any = macro implementations.join_impl[LHS, RHS]
  }
 
  object implementations {
  	import scala.language.existentials
  	def join_impl[LHS <: Product : c.WeakTypeTag, RHS <: Product : c.WeakTypeTag](c: Context)(rhs: c.Expr[RHS]) = {
  		import c.universe._
  		val rMacros = new Macros.RecordMacros[c.type](c)
  		val conv = new ConversionMacros[c.type](c)

  		// check if it is a case class   		
  		val lhsType = weakTypeTag[LHS].tpe
      val lhsSym = lhsType.typeSymbol
      val rhsType = weakTypeTag[RHS].tpe
      val rhsSym = lhsType.typeSymbol

      if (!rhsSym.asClass.isCaseClass && !lhsSym.asClass.isCaseClass) {
        c.abort(NoPosition,
          "Currently, only case classes can be joined into.")
      }

      // get the lhs
      val Apply(TypeApply(Select(Select(_, _), TermName("JoinR")), List(TypeTree())), List(lhsTree)) = c.prefix.tree      
      val rhsTree = rhs.tree
      val (lhsVal, rhsVal) = (TermName(c.fresh("lhs$")), TermName(c.fresh("rhs$")))

      val lhsFields: List[(String, c.Type)]  = conv.caseClassFields(lhsType)
      val rhsFields: List[(String, c.Type)]  = conv.caseClassFields(rhsType)
      
      val commonFields = (lhsFields ++ rhsFields).distinct
      def fieldConstructors(fields: List[(String, c.Type)], v: TermName): List[Tree] = 
        fields.map(_._1.toString).map(name => q"${Literal(Constant(name))} -> $v.${TermName(name)}")
      val constructor = fieldConstructors(lhsFields, lhsVal) ++ fieldConstructors(rhsFields, rhsVal)      
  		
  		// make a record with a combination of fields
  		q"""
  		  val $lhsVal = $lhsTree
  		  val $rhsVal = $rhsTree
  		  ${rMacros.record(commonFields)(c.Expr(q"""Map(..$constructor)""")).tree}
  		"""  		  		
  	}
  }
}