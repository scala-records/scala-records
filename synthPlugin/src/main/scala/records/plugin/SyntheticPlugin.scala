package records.plugin

import scala.tools.nsc._
import scala.tools.nsc.plugins._

import scala.reflect.internal.Flags

/** Tiny plugin to mark __data methods on records.Rec as synthetic */
class SyntheticPlugin(val global: Global) extends Plugin {

  val name = "rsynth"
  val description = "Mark __data methods on records.Rec as synthetic"
  val components: List[PluginComponent] = List(RSynthComponent)

  object RSynthComponent extends PluginComponent with transform.Transform {
    override val runsAfter = List("typer")
    override val runsBefore = List("pickle")

    val global: SyntheticPlugin.this.global.type = SyntheticPlugin.this.global
    val phaseName = "rsynth"

    import global._

    override protected def newTransformer(unit: CompilationUnit) =
      new RSynthTransformer(unit)

    class RSynthTransformer(unit: CompilationUnit) extends Transformer {
      private def needsSynthetic(sym: Symbol) =
        sym.fullName.startsWith("records.Rec.__data")

      override def transform(tree: Tree): Tree = tree match {
        case dd: DefDef if needsSynthetic(dd.symbol) =>
          dd.symbol.setFlag(Flags.SYNTHETIC)
          dd
        case t => super.transform(t)
      }
    }
  }

}
