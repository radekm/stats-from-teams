name := "stats-from-teams"

version := "0.1"

scalaVersion := "2.13.4"

resolvers += "snapshots" at "https://oss.jfrog.org/artifactory/oss-snapshot-local"

libraryDependencies ++= Seq(
  "io.monix" %% "monix" % "3.3.0",
  "com.microsoft.azure" % "msal4j" % "1.8.0",
  "com.microsoft.graph" % "microsoft-graph-beta" % "0.1.0-SNAPSHOT",
  "org.jsoup" % "jsoup" % "1.13.1",
  "org.slf4j" % "slf4j-nop" % "1.8.0-beta4",
)

val circeVersion = "0.13.0"

libraryDependencies ++= Seq(
  "io.circe" %% "circe-core",
  "io.circe" %% "circe-generic-extras",
  "io.circe" %% "circe-parser"
).map(_ % circeVersion)
