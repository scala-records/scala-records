scalaVersion := "2.11.1"

libraryDependencies += "org.scala-lang" % "scala-reflect" % "2.11.1"

libraryDependencies += "org.scala-lang" % "scala-compiler" % "2.11.1"

scalaSource in Compile <<= baseDirectory(_ / "src")