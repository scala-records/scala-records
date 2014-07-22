package records.benchmark
import org.scalameter.api._
import records._

object RecordsRuntimeBenchmark extends RecordsBenchmarkSuite {
  // rec is referenced from the generated code
  val rec = Create.Rec(Create.maxSize)

  performance of "Records" in {

    measure method "construction" in {
      using(fieldIndexes) in { x =>
        Create.recSwitch(x, Create.maxSize)
      }
    }

    measure method "field access" in {
      using(fieldIndexes) in { x =>
        Create.accessRecSwitch(x, Create.maxSize, rec)
      }
    }

  }
}
