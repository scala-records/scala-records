import sbt._
import Keys._

object BuildSettings {
  val paradiseVersion = "2.0.0"
  val buildSettings = Defaults.defaultSettings ++ Seq(
    organization := "records",
    version := "0.1-SNAPSHOT",
    //scalacOptions += "-Xlog-implicits",
    //scalacOptions += "-Xprint:cleanup",
    scalacOptions ++= Seq("-deprecation", "-feature"),
    autoAPIMappings := true,
    scalaVersion := "2.11.1",
    crossScalaVersions := Seq("2.10.2", "2.10.3", "2.10.4", "2.11.0", "2.11.1"),
    libraryDependencies += "org.scalatest" %% "scalatest" % "2.2.0" % "test"
  )

  val macroBuildSettings = buildSettings ++ Seq(
    libraryDependencies <+= (scalaVersion)("org.scala-lang" % "scala-reflect" % _),
    libraryDependencies ++= {
      if (scalaVersion.value.startsWith("2.10")) Seq(
          compilerPlugin("org.scalamacros" % "paradise" % "2.0.0" cross CrossVersion.full),
          "org.scalamacros" %% "quasiquotes" % "2.0.0" cross CrossVersion.binary
      ) else Nil
    }
  )
}


object MyBuild extends Build {
  import BuildSettings._

  lazy val root = Project(
    "root",
    file("."),
    settings = buildSettings ++ Seq(
      name := "refined-records",
      run <<= run in Compile in core,
      console <<= console in Compile in core
    )
  ) aggregate(macros, core, common)

  lazy val common = Project(
    "common",
    file("common"),
    settings = macroBuildSettings ++ (
      libraryDependencies <+= (scalaVersion)("org.scala-lang" % "scala-compiler" % _))
  )

  lazy val macros: Project = Project(
    "macros",
    file("macros"),
    settings = macroBuildSettings
  ) dependsOn(common)

  lazy val core = Project(
    "core",
    file("core"),
    settings = buildSettings ++ Seq(
      unmanagedSourceDirectories in Test ++= {
        if (scalaVersion.value.startsWith("2.11"))
          Seq(sourceDirectory.value / "test-2.11" / "scala")
        else
          Seq()
      }
    )
  ) dependsOn(macros, common)
}
