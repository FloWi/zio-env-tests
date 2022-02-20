scalaVersion := "2.13.8"
name := "zio-env-tests"
organization := "de.flwi"
scalafmtOnCompile := true
fork in Test := true
parallelExecution in Test := true

lazy val Versions = new {
  val zio = "2.0.0-RC2"
  val catsEffect = "3.3.5"
}

// Scala libraries
libraryDependencies ++= Seq(
  "dev.zio" %% "zio" % Versions.zio,
  "dev.zio" %% "zio-test" % Versions.zio % "test",
  "dev.zio" %% "zio-test-sbt" % Versions.zio % "test",
  "org.typelevel" %% "cats-effect" % Versions.catsEffect,
  "com.disneystreaming" %% "weaver-cats" % "0.7.10" % Test
)

fork := true

testFrameworks += new TestFramework("zio.test.sbt.ZTestFramework")
testFrameworks += new TestFramework("weaver.framework.CatsEffect")

// Java libraries
libraryDependencies ++= Seq(
  //"ch.qos.logback" % "logback-classic" % Versions.logback
)
