name := """jobcoin-mixer"""

version := "1.0-SNAPSHOT"

lazy val root = (project in file(".")).enablePlugins(PlayScala)

scalaVersion := "2.11.8"

libraryDependencies ++= Seq(
  jdbc,
  cache,
  ws,
  "org.scalatestplus.play" %% "scalatestplus-play" % "1.5.1" % Test,
  "org.typelevel" %% "cats" % "0.9.0",
  "com.typesafe.akka" %% "akka-testkit" % "2.4.17" % Test
)

