package records.benchmark
import org.scalameter.api._
import shapeless._
import syntax.singleton._
import records.benchmark._

object HMapCompileTimeBenchmark extends RecordsBenchmarkSuite with TypecheckingBenchmarkingSuite {

  // hmap is referenced from the generated code
  val hmap = Create.HMap(Create.maxSize)

  def source(fPos: Int) = s"""|
    |import shapeless._
    |import syntax.singleton._
    |object A {
    |  val hmap = records.benchmark.HMapCompileTimeBenchmark.hmap
    |  ${(0 until Create.repetitionsInCompile).map(_ => s"""  hmap.get("f$fPos")""").mkString("\n")}
    |}
    |""".stripMargin

  performance of "HMaps compile time" in {

    measure method s"access size ${Create.maxSize} until typer" in {
      using(fieldIndexes)
        .setUp(_ => setupCompiler(List("-Ystop-after:typer")))
        .in { x => compileSource(source(x)) }
    }

    measure method s"access size ${Create.maxSize}" in {
      using(fieldIndexes)
        .setUp(_ => setupCompiler())
        .in { x => compileSource(source(x)) }
    }

  }
}
