Database Records Based on Structural Refinements [![Build Status](https://travis-ci.org/vjovanov/refined-ch.epfl.png?branch=master)](https://travis-ci.org/vjovanov/refined-records)
================================================

This project is about designing a data type for database ch.epfl. Currently it is in the early stage of development: design decissions are being made in the `design.md` document.

## Current Limitations
### For All Scala Versions

1. Records can not be implicitly converted into case classes. One must explicitly call `r.to[CaseClassName]`.

### For Scala 2.10.x

1. All record calls will fire a warning for a reflective macro call.
   
   ```
[warn] 109: reflective access of structural type member macro method baz should be enabled
[warn] by making the implicit value scala.language.reflectiveCalls visible.
[warn]     row.baz should be (1.7)
   ```

   To disable this users must use an import `import scala.language.reflectiveCalls` or by setting the compiler option `-language:reflectiveCalls`.
2. Least upper bounds (LUBs) of two records can not be found. Consequences are the following: 
   + If two queries return the same records the results can not be directly combined under a same type. For example, `List(R("a" -> 1) R("a" -> 2))` will not be usable.
