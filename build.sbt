ThisBuild / version := "0.1.0-SNAPSHOT"

ThisBuild / scalaVersion := "3.1.3"

lazy val root = (project in file("."))
  .settings(
    libraryDependencies += "org.scalatest" %% "scalatest" % "3.2.13" % Test,
    libraryDependencies += "org.scalatest" %% "scalatest-featurespec" % "3.2.13" % Test,
    name := "CS474"
  )
