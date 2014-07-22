package records
package benchmark

import java.io._

import scala.tools.nsc._
import scala.tools.nsc.util._
import scala.tools.nsc.reporters._
import scala.tools.nsc.io._
import scala.io._
import scala.tools.nsc.interpreter.AbstractFileClassLoader
import scala.reflect.internal.util.BatchSourceFile

trait TypecheckingBenchmarkingSuite {
  var compiler: Global = _
  var cReporter: ConsoleReporter = _

  def settings(args: List[String] = Nil): Settings = {
    val newSettings = new Settings()
    newSettings.classpath.value = this.getClass.getClassLoader match {
      case ctx: java.net.URLClassLoader => ctx.getURLs.map(_.getPath).mkString(":")
      case _                            => System.getProperty("java.class.path")
    }
    newSettings.bootclasspath.value = Predef.getClass.getClassLoader match {
      case ctx: java.net.URLClassLoader => ctx.getURLs.map(_.getPath).mkString(":")
      case _                            => System.getProperty("sun.boot.class.path")
    }
    newSettings.encoding.value = "UTF-8"
    newSettings.outdir.value = "."
    newSettings.extdirs.value = ""
    newSettings.processArguments(args, true)
    newSettings
  }

  def setupCompiler(args: List[String] = Nil): Unit = {
    cReporter = new ConsoleReporter(settings(args), null, new PrintWriter(System.out))
    compiler = new Global(settings(args), cReporter)
    val fileSystem = new VirtualDirectory("<vfs>", None)
    compiler.settings.outputDirs.setSingleOutput(fileSystem)
  }

  def compileSource(source: String): Unit = {
    val comp = compiler
    val run = new comp.Run
    run.compileSources(scala.List(new BatchSourceFile("<stdin>", source)))
    cReporter.printSummary()
  }
}
