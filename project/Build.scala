import sbt._
import Keys._
import com.typesafe.sbt.SbtScalariform
import com.typesafe.sbt.SbtScalariform.ScalariformKeys

import org.scalajs.sbtplugin.ScalaJSPlugin
import ScalaJSPlugin._
import ScalaJSPlugin.autoImport._

object BuildSettings {
  val paradiseVersion = "2.1.0"
  val buildSettings = SbtScalariform.scalariformSettings ++ Seq(

    // Metadata
    version := "0.5-SNAPSHOT",
    organization := "ch.epfl.lamp",
    licenses := Seq("New BSD" -> url("https://raw2.github.com/scala-records/scala-records/master/LICENSE")),
    homepage := Some(url("https://github.com/scala-records/scala-records/")),
    organizationHomepage := Some(url("http://lamp.epfl.ch")),
    scmInfo := Some(ScmInfo(
      url("https://github.com/scala-records/scala-records.git"),
      "git://github.com/scala-records/scala-records.git")),
    pomExtra := (
      <developers>
        <developer>
          <id>gzm0</id>
          <name>Tobias Schlatter</name>
          <url>https://github.com/gzm0</url>
        </developer>
        <developer>
          <id>vjovanov</id>
          <name>Vojin Jovanovic</name>
          <url>https://github.com/vjovanov</url>
        </developer>
        <developer>
          <id>hubertp</id>
          <name>Hubert Plociniczak</name>
          <url>https://github.com/hubertp</url>
        </developer>
      </developers>),

    // Actual settings
    scalacOptions ++= Seq("-deprecation", "-feature"),
    autoAPIMappings := true,
    scalaVersion := "2.11.6",

    crossScalaVersions := Seq(
      "2.10.2", "2.10.3", "2.10.4",
      "2.11.0", "2.11.1", "2.11.2",
      "2.11.3", "2.11.4", "2.11.5",
      "2.11.6", "2.12.0", "2.12.1",
      "2.12.2", "2.12.3"),
    resolvers += Resolver.sonatypeRepo("snapshots"),
    libraryDependencies += {
      "org.scalatest" %% "scalatest" % "3.0.0" % "test"
    },
    ScalariformKeys.preferences in Compile := formattingPreferences,
    ScalariformKeys.preferences in Test    := formattingPreferences
  )

  val publishSettings = Seq(
    publishMavenStyle := true,
    publishTo := {
      val nexus = "https://oss.sonatype.org/"
      if (isSnapshot.value)
        Some("snapshots" at nexus + "content/repositories/snapshots")
      else
        Some("releases" at nexus + "service/local/staging/deploy/maven2")
    },
    publishArtifact in Test := false // just to be safe
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

  val sharedCoreSettings = (
    macroBuildSettings ++ publishSettings
  ) ++ Seq(
    name := "scala-records",
    autoCompilerPlugins := true
  )

  lazy val root = project.in(file("."))
    .aggregate(synthPlugin, core, tests)

  lazy val synthPlugin = project
    .settings(buildSettings: _*)
    .settings(
      exportJars := true,
      crossVersion := CrossVersion.full,
      libraryDependencies ++= Seq(
        "org.scala-lang" % "scala-compiler" % scalaVersion.value,
        "org.scala-lang" % "scala-reflect" % scalaVersion.value)
    )

  lazy val core = project
    .settings(sharedCoreSettings: _*)
    .dependsOn(synthPlugin % "plugin")

  lazy val coreJS = project
    .enablePlugins(ScalaJSPlugin)
    .settings(sharedCoreSettings: _*)
    .settings(scalaSource in Compile <<= scalaSource in core in Compile)
    .dependsOn(synthPlugin % "plugin")

  lazy val tests = project
    .settings(macroBuildSettings: _*)
    .settings(
      name := "scala-records-tests",
      unmanagedSourceDirectories in Test ++= {
        if (scalaVersion.value >= "2.11")
          Seq(sourceDirectory.value / "test-2.11" / "scala")
        else
          Seq(sourceDirectory.value / "test-2.10" / "scala")
      }
    )
    .dependsOn(core)

}
