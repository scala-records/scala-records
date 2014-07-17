package records.test

import org.scalatest._

import Inspectors._
import Assertions._

import records.Rec

import scala.collection.mutable

// This is for 2.10.x compatibility!
import scala.language.reflectiveCalls

class CollectionTest extends FlatSpec with Matchers {

  "A record" should "be tabulated in a list" in {
    val n = 10
    val l = List.tabulate(n)(i => Rec("inc" -> i, "dec" -> (n - i)))

    forAll(l) { x => (x.inc + x.dec) should be(n) }
  }

  it should "work in untyped sets" in {

    val set = mutable.Set.empty[Any]

    set += Rec("a" -> 1)
    set += Rec("b" -> 2)
    set += Rec("Hello World" -> 5, "r" -> Rec("b" -> 3))

    assert(set.contains(Rec("a" -> 1)))
    assert(set.contains(Rec("b" -> 2)))
    assert(set.contains(Rec("Hello World" -> 5, "r" -> Rec("b" -> 3))))

    assert(!set.contains("String"))
    assert(!set.contains(Rec("a" -> 5)))
    assert(!set.contains(Rec("c" -> 1)))
  }

  it should "be used as key in a map" in {
    val map = Map(Rec("a" -> 1) -> "hello", Rec("a" -> 2) -> "world")

    map(Rec("a" -> 1)) should be("hello")
    map(Rec("a" -> 2)) should be("world")

    assert(!map.contains(Rec("a" -> 5)))
    assert(!map.contains(Rec("a" -> 3)))
  }

  it should "be used as key in an untyped map" in {
    val map = mutable.Map.empty[Any, Int]

    map += "Foo" -> 5
    map += Rec("foo" -> "foo") -> 10
    map += Rec("foo" -> Rec("bar" -> "a")) -> 100

    map("Foo") should be(5)
    map(Rec("foo" -> "foo")) should be(10)
    map(Rec("foo" -> Rec("bar" -> "a"))) should be(100)

    assert(!map.contains(1))
    assert(!map.contains("Bar"))
    assert(!map.contains(Rec("bar" -> "foo")))
    assert(!map.contains(Rec("foo" -> "bar")))
  }

  it should "work in scala.Array" in {
    val array = Array(Rec("-" -> 1, "+" -> -1), Rec("-" -> 2, "+" -> -2))

    array.size should be(2)

    array(0) should be(Rec("-" -> 1, "+" -> -1))
    array(1) should be(Rec("-" -> 2, "+" -> -2))
  }

}
