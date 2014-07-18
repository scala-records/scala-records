package records.benchmark
import org.scalameter.api._
import records.Rec

object Create {
  import scala.language.experimental.macros

  def recSwitch(i: Int, size: Int): Any = macro BenchmarkingMacros.recSwitch
  def accessRecSwitch(i: Int, size: Int, rec: Rec): Any = macro BenchmarkingMacros.accessRecSwitch
  def hMapSwitch(i: Int, size: Int): Any = macro BenchmarkingMacros.hMapSwitch
  def accessHMapSwitch(i: Int, size: Int, hmap: Any): Any = macro BenchmarkingMacros.accessHMapSwitch
  def Rec(i: Int): Any = macro BenchmarkingMacros.createRec
  def HMap(i: Int): Any = macro BenchmarkingMacros.createHMap
  // must be inlined by the compiler, i.e. do not put a type annotation.
  final val maxSize = 30
  final val iterationsCount = 100000
  final val repetitionsInCompile = 10
}

trait RecordsBenchmarkSuite extends PerformanceTest.OfflineReport {
  val iterations = 0 until Create.iterationsCount

  val fieldIndexes = Gen.range("Field Count")(1, Create.maxSize, 1)
}
