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
    scalaVersion := "2.11.0",
    crossScalaVersions := Seq("2.10.2", "2.10.3", "2.10.4", "2.11.0"),
    resolvers += Resolver.sonatypeRepo("snapshots"),
    resolvers += Resolver.sonatypeRepo("releases"),
    libraryDependencies += "org.scalatest" % "scalatest_2.11" % "2.1.5" % "test"
    //addCompilerPlugin("org.scalamacros" % "paradise" % paradiseVersion cross CrossVersion.full)
  )

  val macroBuildSettings = buildSettings ++ Seq(
    scalacOptions += "-language:experimental.macros",
    libraryDependencies <+= (scalaVersion)("org.scala-lang" % "scala-reflect" % _),
    libraryDependencies ++= (
      if (scalaVersion.value.startsWith("2.10")) List("org.scalamacros" %% "quasiquotes" % paradiseVersion)
      else Nil
    )
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
    settings = buildSettings
  ) dependsOn(macros, common)
}
