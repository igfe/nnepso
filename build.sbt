ThisBuild / version := "0.1.0-SNAPSHOT"

ThisBuild / scalaVersion := "2.12.10" // highest version allowed by cilib

libraryDependencies ++= Seq(
    "net.cilib" %% "cilib-core" % "2.0.1",
    "net.cilib" %% "cilib-pso" % "2.0.1",
    "net.cilib" %% "cilib-exec" % "2.0.1",
    "net.cilib" %% "benchmarks" % "0.1.1"
)

lazy val root = (project in file("."))
  .settings(
    name := "nnepso"
  )
