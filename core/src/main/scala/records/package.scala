/**
 * == Labeled Records for Scala ==
 *
 * If you just want to create a record and play around with it, you
 * can use [[Rec.apply]]:
 *
 * {{{
 * val data = Rec("name" -> "Tom", age -> 5)
 * }}}
 *
 * You can then access its fields as if it were a normal class:
 *
 * {{{
 * data.name // > "Tom": String
 * data.age  // > 5: Int
 * }}}
 *
 * Note that accesses are type safe. Accessing a field that doesn't
 * exist will result in a compile-time error:
 *
 * {{{
 * data.password // Compile-time error
 * }}}
 *
 * Nested records work, too:
 *
 * {{{
 * val data = Rec("name" -> "Tom", "age" -> 5,
 *                "creds" -> Rec("user" -> "tom", "pw" -> "1234"))
 * data.creds.user // > tom
 * data.creds.pw   // > 1234
 * }}}
 *
 * Note that the type of a record displays nicely:
 * {{{
 * Rec { def name: String, def age: Int }
 * }}}
 *
 * However, you may not explicitly write this type in your program
 * or you will trip the Scala compiler.
 *
 * == Conversion ==
 *
 * You can easily convert records to case classes with matching
 * fields, using the [[Rec.Convert.to]] method which is added on
 * [[Rec]] through an implicit conversion.
 *
 * {{{
 * val data = Rec("name" -> "Tom", "age" -> 5)
 * case class Person(name: String, age: Int)
 * data.to[Person] // > Person("Tom", 5)
 * }}}
 *
 * Nested conversion works as you would expect.
 *
 * Note that conversion is also able to discard fields from the
 * record that the target case class doesn't require:
 *
 * {{{
 * val data = Rec("name" -> "Tom", "age" -> 5, "height" -> 40)
 * case class Person(name: String, age: Int)
 * data.to[Person] // > Person("Tom", 5)
 * }}}
 *
 * == Implicit Conversion ==
 *
 * If you import all members of [[RecordConversions]], records can be
 * converted implicitly to case classes:
 *
 * {{{
 * import RecordConversions._
 * val tom: Person = Rec("name" -> "Tom", "age" -> 5)
 * val persons = List[Person](
 *   Rec("name" -> "Tom", "age" -> 5),
 *   Rec("name" -> "Sally", "age" -> 7))
 * }}}
 *
 * == Your own records ==
 *
 * The records you have seen so far store their data in a
 * `Map[String, Any]` when created, and extract the data out of this
 * map when a member is called.
 *
 * For some applications, it can be useful to declare your own
 * implementation to retrieve the data. Imagine for example a
 * database interface, that returns records from typechecked SQL
 * queries.
 *
 * You may provide your own back-end to records by using the macro
 * library provided by [[Macros.RecordMacros]]. You will want
 * to use one of the following methods to create your record:
 *  - [[Macros.RecordMacros.record]]: Simplest case, used by [[Rec.apply]]
 *  - [[Macros.RecordMacros.specializedRecord]]: Records that are specialized
 *    on primitive types. Avoids boxing.
 *  - [[Macros.RecordMacros.genRecord]]: Generalized record. Probably not
 *    what you want.
 *
 * For an example usage, have a look at the code of
 * [[Macros.apply_impl]] (the macro for [[Rec.apply]]).
 *
 */
package object records
