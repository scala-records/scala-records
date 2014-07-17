Labeled Records for Scala [![Build Status](https://travis-ci.org/scala-records/scala-records.png?branch=master)](https://travis-ci.org/scala-records/scala-records)
================================================

This project is about designing a data type for database records. Currently it is in the early stage of development: design decissions are being made in the `design.md` document.

## Current Limitations
### For All Scala Versions

1. Records must not be explicitly mentioned. In case of explicit mentioning the result will be a run-time exception. In `2.11.x` this would be detected by a warning. For example:

   ```
   val rec: Rec{def x: Int} = Rec("x" -> 1)
   rec.x // throws an exception
   ```
   + Fixing [SI-7340](https://issues.scala-lang.org/browse/SI-7340) would resolve this issue.

2. Records will display nicely in IntelliJ IDEA. IntelliJ does not support whitebox macros.

3. In the Eclipse debugger records can not be debugged when conversions to case classes are used. For this to work the IDE must to understand the behavior of implicit macors.

4. In the Eclipse debugger records display as their underlying data structures. If these structures are optimised it is hard to keep track of the fields. 

### For Scala 2.10.x

1. All record calls will fire a warning for a reflective macro call.

   ```
[warn] 109: reflective access of structural type member macro method baz should be enabled
[warn] by making the implicit value scala.language.reflectiveCalls visible.
[warn]     row.baz should be (1.7)
   ```

   To disable this users must use an import `import scala.language.reflectiveCalls` or by setting the compiler option `-language:reflectiveCalls`.
2. Least upper bounds (LUBs) of two records can not be found. Consequences are the following:

   + If two queries return the same records the results can not be directly combined under a same type. For example, `List(Rec("a" -> 1), Rec("a" -> 2))` will not be usable.
