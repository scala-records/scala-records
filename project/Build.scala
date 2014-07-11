import sbt._
import Keys._
import scalariform.formatter.preferences._

object BuildSettings {
  val paradiseVersion = "2.0.0"
  val buildSettings = Defaults.defaultSettings ++ Seq(
    organization := "records",
    version := "0.1-SNAPSHOT",
    licenses := Seq("New BSD" -> url("https://raw2.github.com/vjovanov/yin-yang/master/LICENSE")),
    scalacOptions ++= Seq("-deprecation", "-feature"),
    organizationHomepage := Some(url("http://lamp.epfl.ch")),
    autoAPIMappings := true,
    scalaVersion := "2.11.1",
    scmInfo := Some(ScmInfo(url("https://github.com/vjovanov/refined-records.git"),"git://github.com/vjovanov/refined-records.git")),
    crossScalaVersions := Seq(
      "2.10.2", "2.10.3", "2.10.4",
      "2.11.0", "2.11.1", "2.11.2-SHAPSHOT",
      "2.12.0-SNAPSHOT"),
    resolvers += Resolver.sonatypeRepo("snapshots"),
    libraryDependencies += {
      if (scalaVersion.value == "2.12.0-SNAPSHOT")
        "org.scalatest" % "scalatest_2.11" % "2.2.0" % "test"
      else
        "org.scalatest" %% "scalatest" % "2.2.0" % "test"
    }
    // SbtScalariform.scalariformSettings
    // ScalariformKeys.preferences in Compile := formattingPreferences,
    // ScalariformKeys.preferences in Test    := formattingPreferences
  )

  val macroBuildSettings = buildSettings ++ Seq(
    libraryDependencies <+= (scalaVersion)("org.scala-lang" % "scala-reflect" % _),
    libraryDependencies ++= {
      if (scalaBinaryVersion.value == "2.10") Seq(
          compilerPlugin("org.scalamacros" % "paradise" % "2.0.0" cross CrossVersion.full),
          "org.scalamacros" %% "quasiquotes" % "2.0.0" cross CrossVersion.binary
      ) else Nil
    }
  )

  def formattingPreferences = {
    import scalariform.formatter.preferences._
    FormattingPreferences()
    .setPreference(RewriteArrowSymbols, false)
    .setPreference(AlignParameters, true)
    .setPreference(AlignSingleLineCaseStatements, true)
  }
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
        if (scalaBinaryVersion.value == "2.11" ||
            scalaVersion.value == "2.12.0-SNAPSHOTS")
          Seq(sourceDirectory.value / "test-2.11" / "scala")
        else
          Seq()
      }
    )
  ) dependsOn(macros, common)
}
