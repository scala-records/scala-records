Records for Scala [![Build Status](https://travis-ci.org/scala-records/scala-records.png?branch=master)](https://travis-ci.org/scala-records/scala-records)
================================================

Scala Records introduce a data type `Rec` for representing record types. Records are convenient for accessing and manipulating structured data with named fields. Records are similar in functionality to [F# records][f#-records] and [shapeless records][shapeless-records]. Most relevant use cases are:
+ Manipulating large tables in big-data frameworks like [Spark][spark] and [Scalding][scalding]
+ Manipulating results of SQL queries
+ Manipulating JSON

Records are implemented using macros and completely blend in the Scala environment. With records:
+ Fields are accessed with a path just like regular case classes (e.g. `rec.country.state`)
+ Type errors are comprehensible and elaborate
+ Auto-completion in the Eclipse IDE works seamlessly
+ Run-time performance is high due to specialization with macros
+ Compile-time performance is high due to the use of macros

[f#-records]: http://msdn.microsoft.com/en-us/library/dd233184.aspx
[shapeless-records]: https://github.com/milessabin/shapeless/blob/master/examples/src/main/scala/shapeless/examples/records.scala
[scalding]: https://github.com/twitter/scalding
[spark]: https://spark.apache.org/

## Quick Start

To create a simple record run:
```scala
import records.Rec

scala> val person = Rec("name" -> "Hannah", "age" -> 30)
person: records.Rec{def name: String; def age: Int} = Rec { name = Hannah, age = 30 }
```

Fields of records can be accessed just like fields of classes:
```scala
if (person.age > 18) println(s"${person.name} is an adult.")
```

Scala Records allow for arbitrary levels of nesting:
```scala
val person = Rec(
 "name" -> "Hannah",
 "age" -> 30,
 "country" -> Rec("name" -> "US", "state" -> "CA"))
```

They can be explicitly converted to case classes:
```scala
case class Country(name: String, state: String)
case class Person(name: String, age: String, country: Country)
val personClass = person.to[Person]
```

As well as implicitly when the contents of `records.RecordConversions` are imported:
```scala
import records.RecordConversions._
val personClass: Person = person
```

In case of erroneous access, type errors will be comprehensible:
```scala
scala> person.nme
<console>:10: error: value nme is not a member of records.Rec{def name: String; def age: Int}
              person.nme
                     ^
```

Errors are also appropriate when converting to case classes:
```scala
val person = Rec("name" -> "Hannah", "age" -> 30)
val personClass = person.to[Person]

<console>:13: error: Converting to Person would require the source record to have the following additional fields: [country: Country].
       val personClass = person.to[Person]
                                  ^
```

## Including Scala Records in Your Project

To include Scala Records in your SBT build please add:

```scala
libraryDependencies += "ch.epfl.lamp" %% "scala-records" % <version>
```

[sonatype]: https://oss.sonatype.org/index.html#nexus-search;quick~scala-records

## Support

It is "safe" to use Scala Records in your project. They will be supported until we find a more principal, and functioning, solution for accessing
structured data in Scala. For further details see [this page][design-decisions].

[design-decisions]: https://github.com/scala-records/scala-records/wiki/Why-Scala-Records-with-Structural-Types-and-Macros%3F
## Current Limitations
### For All Scala Versions

1. Record types must not be explicitly mentioned. In case of explicit mentioning the result will be a run-time exception. In `2.11.x` this would be detected by a warning. For example:

   ```
   val rec: Rec { def x: Int } = Rec("x" -> 1)
   rec.x // throws an exception
   ```
   + Fixing [SI-7340](https://issues.scala-lang.org/browse/SI-7340) would resolve this issue.
   + A workaround would be to write a case class for a record type.

2. Records will not display nicely in IntelliJ IDEA. IntelliJ IDEA does not support whitebox macros:
   + Writing a custom implementation for IntelliJ would remove this limitation.

3. In the Eclipse debugger records can not be debugged when conversions to case classes are used. For this to work the IDE must to understand the behavior of implicit macros.

4. In the Eclipse debugger records display as their underlying data structures. If these structures are optimized it is hard to keep track of the fields.

### For Scala 2.10.x

1. All record calls will fire a warning for a reflective macro call.

   ```
[warn] 109: reflective access of structural type member macro method baz should be enabled
[warn] by making the implicit value scala.language.reflectiveCalls visible.
[warn]     row.baz should be (1.7)
   ```

   To disable this warning users must introduce `import scala.language.reflectiveCalls` in a scope or set the compiler option `-language:reflectiveCalls`.
2. Least upper bounds (LUBs) of two records can not be found. Consequences are the following:

   + If two queries return the same records the results can not be directly combined under a same type. For example, `List(Rec("a" -> 1), Rec("a" -> 2))` will not be usable.

## Helping Further Development

In case you have any desires for new functionality, or find errors in the existing one, please report them in the [issue tracker][issues]. We will gladly discuss further development and accept your pull requests.

[issues]: https://github.com/scala-records/scala-records/issues

## Contributors
Scala Records are developed with love and joy in the [Scala Lab][scalalab] at [EPFL][epfl] in collaboration with Michael Armbrust from [Databricks][databricks]. Main contributors are:
 + Vojin Jovanovic (@vjovanov)
 + Tobias Schlatter (@gzm0)
 + Hubert Plocziniczak (@hubertp)

[databricks]: https://databricks.com/
[scalalab]: http://lamp.epfl.ch/
[epfl]: http://epfl.ch/
