import reflect.runtime.universe._
import reflect.runtime.{currentMirror => m}
import scala.tools.reflect.ToolBox
import Flag._
trait Rec

object Main extends App {
  type Fields = (String, Type)
  type Schema = List[Fields]
  def create(s: Schema): Tree = {
    def  fieldTree(field: (String, reflect.runtime.universe.Type)) = ValDef(Modifiers(), TermName(field._1), Ident(TypeName(field._2.toString)), TypeApply(Select(Apply(Ident(TermName("list")), List(Literal(Constant(0)))), TermName("asInstanceOf")), List(Ident(TypeName(field._2.toString)))))
    val const = DefDef(Modifiers(), termNames.CONSTRUCTOR, List(), List(List()), TypeTree(), Block(Nil, Literal(Constant(()))))
    val fields = s.map(fieldTree)
 	Function(
 		List(ValDef(Modifiers(PARAM), TermName("list"), AppliedTypeTree(Ident(TypeName("List")), List(Ident(TypeName("Any")))), EmptyTree)),
 		 Block(List(ClassDef(Modifiers(FINAL), TypeName("$anon"), List(),
 		 	Template(List(Ident(TypeName("Rec"))), noSelfType, const :: fields))),
 		    Apply(Select(New(Ident(TypeName("$anon"))), termNames.CONSTRUCTOR), List())))
  }



  val tree = create(("phone", typeOf[String]) :: ("age", typeOf[Int]) :: Nil)

  val tb = m.mkToolBox()
  println(tb.typecheck(tree))
}
