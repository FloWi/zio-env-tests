scalaVersion := "2.13.8"
name := "zio-env-tests"
organization := "de.flwi"
scalafmtOnCompile := true
fork in Test := true
parallelExecution in Test := true

lazy val Versions = new {
  val zio = "2.0.0-RC2"
}

// Scala libraries
libraryDependencies ++= Seq(
  "dev.zio" %% "zio" % Versions.zio,
  "dev.zio" %% "zio-test" % Versions.zio % "test",
  "dev.zio" %% "zio-test-sbt" % Versions.zio % "test"
)

fork := true

testFrameworks += new TestFramework("zio.test.sbt.ZTestFramework")

// Java libraries
libraryDependencies ++= Seq(
  //"ch.qos.logback" % "logback-classic" % Versions.logback
)
