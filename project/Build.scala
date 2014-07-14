import sbt._
import Keys._
import com.typesafe.sbt.SbtScalariform
import com.typesafe.sbt.SbtScalariform.ScalariformKeys

object BuildSettings {
  val paradiseVersion = "2.0.0"
  val buildSettings = Defaults.defaultSettings ++ SbtScalariform.scalariformSettings ++ Seq(
    organization := "records",
    version := "0.1-SNAPSHOT",
    licenses := Seq("New BSD" -> url("https://raw2.github.com/vjovanov/refined-records/master/LICENSE")),
    scalacOptions ++= Seq("-deprecation", "-feature"),
    organizationHomepage := Some(url("http://lamp.epfl.ch")),
    autoAPIMappings := true,
    scalaVersion := "2.11.1",
    scmInfo := Some(ScmInfo(url("https://github.com/vjovanov/refined-records.git"),"git://github.com/vjovanov/refined-records.git")),
    crossScalaVersions := Seq(
      "2.10.2", "2.10.3", "2.10.4",
      "2.11.0", "2.11.1",
      "2.12.0-SNAPSHOT"),
    resolvers += Resolver.sonatypeRepo("snapshots"),
    libraryDependencies += {
      if (scalaVersion.value == "2.12.0-SNAPSHOT")
        "org.scalatest" % "scalatest_2.11" % "2.2.0" % "test"
      else
        "org.scalatest" %% "scalatest" % "2.2.0" % "test"
    },
    ScalariformKeys.preferences in Compile := formattingPreferences,
    ScalariformKeys.preferences in Test    := formattingPreferences
  )

  val macroBuildSettings = buildSettings ++ Seq(
    libraryDependencies <+= (scalaVersion)("org.scala-lang" % "scala-reflect" % _),
    libraryDependencies ++= {
      if (scalaBinaryVersion.value == "2.10") Seq(
          compilerPlugin("org.scalamacros" % "paradise" % paradiseVersion cross CrossVersion.full),
          "org.scalamacros" %% "quasiquotes" % paradiseVersion cross CrossVersion.binary
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

  lazy val root = project.in(file(".")).aggregate(core, tests)

  lazy val core = project
    .settings(macroBuildSettings: _*)
    .settings(name := "Refined-Records Core")

  lazy val tests = project
    .settings(macroBuildSettings: _*)
    .settings(
      name := "Refined-Records Tests",
      unmanagedSourceDirectories in Test ++= {
        if (scalaBinaryVersion.value == "2.11" ||
            scalaVersion.value == "2.12.0-SNAPSHOTS")
          Seq(sourceDirectory.value / "test-2.11" / "scala")
        else
          Seq()
      }
    )
    .dependsOn(core)

}

