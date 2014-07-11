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
    }
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
}


object MyBuild extends Build {
  import BuildSettings._

  lazy val refinedRecords = Project(
    "refinedRecords",
    file("."),
    settings = macroBuildSettings ++ Seq(
      name := "refined-records",
      unmanagedSourceDirectories in Test ++= {
        if (scalaBinaryVersion.value == "2.11" ||
            scalaVersion.value == "2.12.0-SNAPSHOTS")
          Seq(sourceDirectory.value / "test-2.11" / "scala")
        else
          Seq()
      }
    )
  )

}
