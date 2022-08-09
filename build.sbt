// logLevel := Level.Warn

inThisBuild(
  List(
    version := "1.0",
    scalaVersion := "2.12.15",
    run / fork := true,
    organization := "com.example"
  )
)

// mainClass := Some("com.example.main")
cleanFiles += baseDirectory.value / "out/"

Global / onChangedBuildSource := ReloadOnSourceChanges

//val cilibVersion = "2.0.0+97-d11405f0-SNAPSHOT"
val cilibVersion = "2.0.0+123-6fb77f57-SNAPSHOT"
val benchmarksVersion = "0.2.0-SNAPSHOT"

lazy val root = (project in file("."))
  .settings(
    name := "nnepso",
    resolvers += Resolver.sonatypeRepo("snapshots"),
    scalacOptions += "-Ywarn-unused",
    libraryDependencies ++= Seq(
      "net.cilib" %% "core" % cilibVersion,
      "net.cilib" %% "pso" % cilibVersion,
      "net.cilib" %% "io" % cilibVersion,
      "net.cilib" %% "exec" % cilibVersion,
      "net.cilib" %% "benchmarks" % benchmarksVersion,
      "dev.zio" %% "zio-cli" % "0.2.7",
      compilerPlugin(
        "org.typelevel" %% "kind-projector" % "0.13.2" cross CrossVersion.full
      )
    )
  )
