package records.test

import org.scalatest._

// This is for 2.10.x compatibility!
import scala.language.reflectiveCalls

abstract class Creature { def name: String; def age: Int }
case class Person(name: String, age: Int) extends Creature
case class Employee(p: Person, title: String)
class Mother(val p: Person, val numChildren: Int)
case class Dog(name: String, age: Int, favoriteTreat: String) extends Creature

class FieldsAsArraysTest extends FlatSpec with Matchers {
  import records.Rec

  // Arrays of primitives
  "A Record" should "be able to have Array[Int]s as fields" in {
    val a = Rec("arr" -> Array(1, 2, 3))

    a.arr should be(Array(1, 2, 3))
  }

  it should "be able to have Array[Doubles]s as fields" in {
    val a = Rec("arr" -> Array(1.0, 2.0, 3.0))

    a.arr should be(Array(1.0, 2.0, 3.0))
  }

  it should "be able to have Array[Byte]s as fields" in {
    val a = Rec("arr" -> Array(1.toByte, 2.toByte, 3.toByte))

    a.arr should be(Array(1.toByte, 2.toByte, 3.toByte))
  }

  it should "be able to have Array[Short]s as fields" in {
    val a = Rec("arr" -> Array(1.toShort, 2.toShort, 3.toShort))

    a.arr should be(Array(1.toShort, 2.toShort, 3.toShort))
  }

  it should "be able to have Array[Long]s as fields" in {
    val a = Rec("arr" -> Array(1L, 2L, 3L))

    a.arr should be(Array(1L, 2L, 3L))
  }

  it should "be able to have Array[Float]s as fields" in {
    val a = Rec("arr" -> Array(1F, 2F, 3F))

    a.arr should be(Array(1F, 2F, 3F))
  }

  it should "be able to have Array[Boolean]s as fields" in {
    val a = Rec("arr" -> Array(true, true, false))

    a.arr should be(Array(true, true, false))
  }

  it should "be able to have Array[String]s as fields" in {
    val a = Rec("arr" -> Array("one", "two", "three"))

    a.arr should be(Array("one", "two", "three"))
  }

  // Arrays of arbitrary objects
  it should "be able to have fields that are arrays of arbitrary case class instances" in {
    val a = Rec("arr" -> Array(Person("Bill", 12), Person("Sally", 65), Person("Jose", 41)))

    a.arr should be(Array(Person("Bill", 12), Person("Sally", 65), Person("Jose", 41)))
    a.arr(0).name should be("Bill")
    a.arr(1).name should be("Sally")
    a.arr(2).name should be("Jose")
  }

  it should "be able to have fields that are arrays of arbitrary, nested, case class instances" in {
    val a = Rec("arr" -> Array(Employee(Person("Jim", 23), "CEO"), Employee(Person("Charles", 22), "Janitor")))

    a.arr(0).p.name should be("Jim")
    a.arr(0).p.age should be(23)
    a.arr(0).title should be("CEO")

    a.arr(1).p.name should be("Charles")
    a.arr(1).p.age should be(22)
    a.arr(1).title should be("Janitor")
  }

  it should "be able to have fields that are arrays of arbitrary class instances" in {
    val m1 = new Mother(Person("Kim", 29), 1)
    val m2 = new Mother(Person("Abagail", 55), 3)
    val a = Rec("arr" -> Array(m1, m2))

    a.arr(0).p.name should be("Kim")
    a.arr(0).numChildren should be(1)

    a.arr(1).p.name should be("Abagail")
    a.arr(1).numChildren should be(3)
  }

  it should "be able to have fields that are arrays of arbitrary case class instances bounded by some type" in {
    val p = Person("Fred", 12)
    val d = Dog("Fido", 3, "bone")
    val a = Rec("arr" -> Array(p, d))

    a.arr(0).name should be("Fred")
    a.arr(0).age should be(12)

    a.arr(1) should be(Dog("Fido", 3, "bone"))
  }

  // Arrays of nested arrays
  it should "be able to have Array[Array[Int]]s as fields" in {
    val a = Rec("arr" -> Array(Array(1, 2, 3)))

    a.arr(0) should be(Array(1, 2, 3))
  }

  it should "be able to have Array[Array[Array[Int]]]s as fields" in {
    val a = Rec("arr" -> Array(Array(Array(1, 2, 3))))

    a.arr(0)(0) should be(Array(1, 2, 3))
  }

}
