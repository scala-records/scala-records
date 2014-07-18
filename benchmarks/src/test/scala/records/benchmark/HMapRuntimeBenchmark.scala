package records.benchmark
import org.scalameter.api._
import shapeless._
import record._
import syntax.singleton._
import test._

object HMapsRuntimeBenchmark extends RecordsBenchmarkSuite {
  // hmap is referenced from the generated code
  val hmap = Create.HMap(Create.maxSize)

  performance of "HMaps" in {

    measure method "construction" in {
      using(fieldIndexes) in { x =>
        Create.hMapSwitch(x, Create.maxSize)
      }
    }

    measure method "field access" in {
      using(fieldIndexes) in { x =>
        Create.accessHMapSwitch(x, Create.maxSize, hmap)
      }
    }

  }
}
