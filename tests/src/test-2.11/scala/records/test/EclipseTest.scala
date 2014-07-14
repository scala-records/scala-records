package records.test

import org.scalatest._

import records.R

/* 
 * Purpose of this file is to see how records behave in the IDE.
 */
final private class EclipseTest {
  
  case class ObjectVal(myObject: AnyRef)
  case class DBRecord(name: String, age: Int, location: String)
  
  // This test is used to explore the macro expansion in the IDE
  val x =  List(
    R("myObject" -> "String", "foo" -> "bar"), R("myObject" -> "String", "foo" -> "bar"),
    R("myObject" -> "String", "foo" -> "bar"), R("myObject" -> "String", "foo" -> "bar"),
    R("myObject" -> "String", "foo" -> "bar"), R("myObject" -> "String", "foo" -> "bar"),
    R("myObject" -> "String", "foo" -> "bar"), R("myObject" -> "String", "foo" -> "bar"),
    R("myObject" -> "String", "foo" -> "bar"), R("myObject" -> "String", "foo" -> "bar"),
    R("myObject" -> "String", "foo" -> "bar"), R("myObject" -> "String", "foo" -> "bar"),
    R("myObject" -> "String", "foo" -> "bar"), R("myObject" -> "String", "foo" -> "bar"),
    R("myObject" -> "String", "foo" -> "bar"), R("myObject" -> "String", "foo" -> "bar"),
    R("myObject" -> "String", "foo" -> "bar"), R("myObject" -> "String", "foo" -> "bar"),
    R("myObject" -> "String", "foo" -> "bar"), R("myObject" -> "String", "foo" -> "bar"),
    R("myObject" -> "String", "foo" -> "bar"), R("myObject" -> "String", "foo" -> "bar"),
    R("myObject" -> "String", "foo" -> "bar"), R("myObject" -> "String", "foo" -> "bar"),
    R("myObject" -> "String", "foo" -> "bar"), R("myObject" -> "String", "foo" -> "bar"),
    R("myObject" -> "String", "foo" -> "bar"), R("myObject" -> "String", "foo" -> "bar"),
    R("myObject" -> "String", "foo" -> "bar"), R("myObject" -> "String", "foo" -> "bar"),
    R("myObject" -> "String", "foo" -> "bar"), R("myObject" -> "String", "foo" -> "bar"),
    R("myObject" -> "String", "foo" -> "bar"), R("myObject" -> "String", "foo" -> "bar"),
    R("myObject" -> "String", "foo" -> "bar"), R("myObject" -> "String", "foo" -> "bar"),
    R("myObject" -> "String", "foo" -> "bar"), R("myObject" -> "String", "foo" -> "bar"),
    R("myObject" -> "String", "foo" -> "bar"), R("myObject" -> "String", "foo" -> "bar"),
    R("myObject" -> "String", "foo" -> "bar"), R("myObject" -> "String", "foo" -> "bar"),
    R("myObject" -> "String", "foo" -> "bar"), R("myObject" -> "String", "foo" -> "bar"),
    R("myObject" -> "String", "foo" -> "bar"), R("myObject" -> "String", "foo" -> "bar"),
    R("myObject" -> "String", "foo" -> "bar"), R("myObject" -> "String", "foo" -> "bar")
  )  
  val y = x.head
  val z = x.head.to[ObjectVal]
  val a = if (true) R("a" -> 1, "b" -> 1) else R("a" -> 1)
  println(a.a)
  
  import records.RecordConversions._  
  val list = List[DBRecord](
    R("name" -> "David", "age" -> 3, "location" -> "Lausanne"),
    R("name" -> "David", "age" -> 4, "location" -> "Lausanne"),
    R("name" -> "David", "age" -> 5, "location" -> "Lausanne"),
    R("name" -> "David", "age" -> 6, "location" -> "Lausanne"),
    R("name" -> "David", "age" -> 7, "location" -> "Lausanne"),
    R("name" -> "David", "age" -> 8, "location" -> "Lausanne"),
    R("name" -> "David", "age" -> 9, "location" -> "Lausanne"),
    R("name" -> "David", "age" -> 10, "location" -> "Lausanne"),
    R("name" -> "David", "age" -> 11, "location" -> "Lausanne"),
    R("name" -> "David", "age" -> 12, "location" -> "Lausanne"),
    R("name" -> "David", "age" -> 13, "location" -> "Lausanne"),
    R("name" -> "David", "age" -> 14, "location" -> "Lausanne"),
    R("name" -> "David", "age" -> 15, "location" -> "Lausanne"),
    R("name" -> "David", "age" -> 16, "location" -> "Lausanne"),
    R("name" -> "David", "age" -> 17, "location" -> "Lausanne"),
    R("name" -> "David", "age" -> 18, "location" -> "Lausanne")
  )
   
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
    y.myObject, y.myObject, y.myObject, y.myObject, y.myObject, y.myObject, y.myObject, y.myObject, y.myObject
  )   
}
