name := "db-op"

version := "0.1"

scalaVersion := "2.12.14"

libraryDependencies ++= Seq(
  "com.h2database" % "h2" % "1.4.200",
  "org.scalatest" %% "scalatest" % "3.0.5" % "test",

)