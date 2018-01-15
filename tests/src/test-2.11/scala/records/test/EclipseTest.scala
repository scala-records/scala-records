package records.test

import org.scalatest._

import records.Rec

/*
 * Purpose of this file is to see how records behave in the IDE.
 */
object EclipseTest extends App {

  case class ObjectVal(myObject: AnyRef)
  case class DBRecord(name: String, age: Int, location: String)

  // This test is used to explore the macro expansion in the IDE
  val x = List(
    Rec("myObject" -> "String", "foo" -> "bar"), Rec("myObject" -> "String", "foo" -> "bar"),
    Rec("myObject" -> "String", "foo" -> "bar"), Rec("myObject" -> "String", "foo" -> "bar"),
    Rec("myObject" -> "String", "foo" -> "bar"), Rec("myObject" -> "String", "foo" -> "bar"),
    Rec("myObject" -> "String", "foo" -> "bar"), Rec("myObject" -> "String", "foo" -> "bar"),
    Rec("myObject" -> "String", "foo" -> "bar"), Rec("myObject" -> "String", "foo" -> "bar"),
    Rec("myObject" -> "String", "foo" -> "bar"), Rec("myObject" -> "String", "foo" -> "bar"),
    Rec("myObject" -> "String", "foo" -> "bar"), Rec("myObject" -> "String", "foo" -> "bar"),
    Rec("myObject" -> "String", "foo" -> "bar"), Rec("myObject" -> "String", "foo" -> "bar"),
    Rec("myObject" -> "String", "foo" -> "bar"), Rec("myObject" -> "String", "foo" -> "bar"),
    Rec("myObject" -> "String", "foo" -> "bar"), Rec("myObject" -> "String", "foo" -> "bar"),
    Rec("myObject" -> "String", "foo" -> "bar"), Rec("myObject" -> "String", "foo" -> "bar"),
    Rec("myObject" -> "String", "foo" -> "bar"), Rec("myObject" -> "String", "foo" -> "bar"),
    Rec("myObject" -> "String", "foo" -> "bar"), Rec("myObject" -> "String", "foo" -> "bar"),
    Rec("myObject" -> "String", "foo" -> "bar"), Rec("myObject" -> "String", "foo" -> "bar"),
    Rec("myObject" -> "String", "foo" -> "bar"), Rec("myObject" -> "String", "foo" -> "bar"),
    Rec("myObject" -> "String", "foo" -> "bar"), Rec("myObject" -> "String", "foo" -> "bar"),
    Rec("myObject" -> "String", "foo" -> "bar"), Rec("myObject" -> "String", "foo" -> "bar"),
    Rec("myObject" -> "String", "foo" -> "bar"), Rec("myObject" -> "String", "foo" -> "bar"),
    Rec("myObject" -> "String", "foo" -> "bar"), Rec("myObject" -> "String", "foo" -> "bar"),
    Rec("myObject" -> "String", "foo" -> "bar"), Rec("myObject" -> "String", "foo" -> "bar"),
    Rec("myObject" -> "String", "foo" -> "bar"), Rec("myObject" -> "String", "foo" -> "bar"),
    Rec("myObject" -> "String", "foo" -> "bar"), Rec("myObject" -> "String", "foo" -> "bar"),
    Rec("myObject" -> "String", "foo" -> "bar"), Rec("myObject" -> "String", "foo" -> "bar"),
    Rec("myObject" -> "String", "foo" -> "bar"), Rec("myObject" -> "String", "foo" -> "bar"))
  val y = x.head
  val z = x.head.to[ObjectVal]
  val a = if (true) Rec("a" -> 1, "b" -> 1) else Rec("a" -> 1)
  println(a.a)

  import records.RecordConversions._
  val list = List[DBRecord](
    Rec("name" -> "David", "age" -> 3, "location" -> "Lausanne"),
    Rec("name" -> "David", "age" -> 4, "location" -> "Lausanne"),
    Rec("name" -> "David", "age" -> 5, "location" -> "Lausanne"),
    Rec("name" -> "David", "age" -> 6, "location" -> "Lausanne"),
    Rec("name" -> "David", "age" -> 7, "location" -> "Lausanne"),
    Rec("name" -> "David", "age" -> 8, "location" -> "Lausanne"),
    Rec("name" -> "David", "age" -> 9, "location" -> "Lausanne"),
    Rec("name" -> "David", "age" -> 10, "location" -> "Lausanne"),
    Rec("name" -> "David", "age" -> 11, "location" -> "Lausanne"),
    Rec("name" -> "David", "age" -> 12, "location" -> "Lausanne"),
    Rec("name" -> "David", "age" -> 13, "location" -> "Lausanne"),
    Rec("name" -> "David", "age" -> 14, "location" -> "Lausanne"),
    Rec("name" -> "David", "age" -> 15, "location" -> "Lausanne"),
    Rec("name" -> "David", "age" -> 16, "location" -> "Lausanne"),
    Rec("name" -> "David", "age" -> 17, "location" -> "Lausanne"),
    Rec("name" -> "David", "age" -> 18, "location" -> "Lausanne"))

  List(
    y.myObject, y.myObject, y.myObject, y.myObject, y.myObject, y.myObject, y.myObject, y.myObject, y.myObject,
    y.myObject, y.myObject, y.myObject, y.myObject, y.myObject, y.myObject, y.myObject, y.myObject, y.myObject,
    y.myObject, y.myObject, y.myObject, y.myObject, y.myObject, y.myObject, y.myObject, y.myObject, y.myObject,
    y.myObject, y.myObject, y.myObject, y.myObject, y.myObject, y.myObject, y.myObject, y.myObject, y.myObject,
    y.myObject, y.myObject, y.myObject, y.myObject, y.myObject, y.myObject, y.myObject, y.myObject, y.myObject,
    y.myObject, y.myObject, y.myObject, y.myObject, y.myObject, y.myObject, y.myObject, y.myObject, y.myObject,
    y.myObject, y.myObject, y.myObject, y.myObject, y.myObject, y.myObject, y.myObject, y.myObject, y.myObject,
    y.myObject, y.myObject, y.myObject, y.myObject, y.myObject, y.myObject, y.myObject, y.myObject, y.myObject,
    y.myObject, y.myObject, y.myObject, y.myObject, y.myObject, y.myObject, y.myObject, y.myObject, y.myObject,
    y.myObject, y.myObject, y.myObject, y.myObject, y.myObject, y.myObject, y.myObject, y.myObject, y.myObject,
    y.myObject, y.myObject, y.myObject, y.myObject, y.myObject, y.myObject, y.myObject, y.myObject, y.myObject,
    y.myObject, y.myObject, y.myObject, y.myObject, y.myObject, y.myObject, y.myObject, y.myObject, y.myObject)
}
