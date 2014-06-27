import reflect.runtime.universe._
import reflect.runtime.{currentMirror => m}
import scala.tools.reflect.ToolBox
import Flag._
trait Rec

object Main extends App {
  type Fields = (String, Type)
  type Schema = List[Fields]
  def create(s: Schema): Tree = {
    def  fieldTree(field: (String, reflect.runtime.universe.Type)) =
      ValDef(Modifiers(), TermName(field._1), Ident(TypeName(field._2.toString)),
          TypeApply(Select(Apply(Ident(TermName("list")), List(Literal(Constant(0)))), TermName("asInstanceOf")), List(Ident(TypeName(field._2.toString)))))
    val const = DefDef(Modifiers(), termNames.CONSTRUCTOR, List(), List(List()), TypeTree(), Block(List(pendingSuperCall), Literal(Constant(()))))
    val fields = s.map(fieldTree)
   	val fun = 
     Function(
   		List(ValDef(Modifiers(PARAM), TermName("list"), AppliedTypeTree(Ident(TypeName("List")), List(Ident(TypeName("Any")))), EmptyTree)),
   		 Block(List(ClassDef(Modifiers(FINAL), TypeName("$anon"), List(),
   		 	Template(List(Ident(TypeName("Rec"))), noSelfType, const :: fields))),
   		    Apply(Select(New(Ident(TypeName("$anon"))), termNames.CONSTRUCTOR), List())))
    fun
  }

  def prog(fun: Tree, values: Tree, name: String) = {
    val recTrait = ClassDef(Modifiers(ABSTRACT | INTERFACE | DEFAULTPARAM | TRAIT),
                            TypeName("Rec"), List(),
                              Template(List(Select(Ident("scala"), TypeName("AnyRef"))), noSelfType, List()))
    Block(List(recTrait), Select(Apply(fun, values :: Nil), name))
  }



  val tree = create(("phone", typeOf[String]) :: ("age", typeOf[Int]) :: Nil)
  val values = q"""("072929292", 12) :: ("138838383", 0) :: Nil"""
  val exec = prog(tree, values, "what")

  val tb = m.mkToolBox()
  val programs = (prog(tree, values, "what"), false) ::
                 (prog(tree, values, "phone"), true) :: Nil
  programs foreach { case (prog, correct) =>
    try {
      println("Record select: ")
      println(showCode(prog))
      tb.typecheck(prog)
      if (!correct) throw new Exception("fail")
    } catch {
      case (_: scala.tools.reflect.ToolBoxError) if !correct => ()
    }
  }
}
