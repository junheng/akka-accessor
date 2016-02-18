import CommonDependency.dependencies

lazy val root = (project in file("."))
  .settings(
    name := "akka-accessor",
    version := "0.1-SNAPSHOT",
    scalaVersion := "2.11.7",
    libraryDependencies ++= dependencies.akka,
    libraryDependencies ++= dependencies.spary,
    libraryDependencies ++= dependencies.json
  )
