organization in ThisBuild := "io.github.junheng.akka"

val versionOfAkka = "2.4.7"

val versionOfSpray = "1.3.3"

val versionOfJson4s = "3.2.11"

lazy val root = (project in file("."))
  .settings(
    name := "akka-accessor",
    version := "0.1-SNAPSHOT",
    scalaVersion := "2.11.7",
    libraryDependencies ++= Seq(
      //spray
      "io.spray" %% "spray-can" % versionOfSpray,
      "io.spray" %% "spray-httpx" % versionOfSpray,
      "io.spray" %% "spray-http" % versionOfSpray,
      //json
      "org.json4s" %% "json4s-jackson" % versionOfJson4s,
      "org.json4s" %% "json4s-ext" % versionOfJson4s,
      "org.json4s" % "json4s-native_2.11" % versionOfJson4s,
      //akka
      "com.typesafe.akka" %% "akka-actor" % versionOfAkka,
      "com.typesafe.akka" %% "akka-cluster" % versionOfAkka,
      "com.typesafe.akka" %% "akka-kernel" % versionOfAkka,
      "com.typesafe.akka" %% "akka-slf4j" % versionOfAkka,
      "com.typesafe.akka" %% "akka-contrib" % versionOfAkka,
      "com.typesafe.akka" %% "akka-testkit" % versionOfAkka
    )
  )
