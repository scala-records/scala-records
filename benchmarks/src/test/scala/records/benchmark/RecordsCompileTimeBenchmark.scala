package records.benchmark
import org.scalameter.api._
import scala.tools.nsc.util._
import records.Rec

object RecordsCompileTimeBenhcmark extends RecordsBenchmarkSuite with TypecheckingBenchmarkingSuite {

  // rec is referenced from the generated code
  val rec = Create.Rec(Create.maxSize)

  def source(fPos: Int) = s"""|
    |import records.Rec
    |object A {
    |  val rec = records.benchmark.RecordsCompileTimeBenhcmark.rec
    |  ${(0 until Create.repetitionsInCompile).map(_ => "  rec.f" + fPos).mkString("\n")}
    |}
    |""".stripMargin

  performance of "Records compile time" in {

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
