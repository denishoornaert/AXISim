ThisBuild / version := "1.0"
ThisBuild / scalaVersion := "2.13.16"
ThisBuild / organization := "org.example"

val spinalVersion = "1.12.3"
val spinalCore = "com.github.spinalhdl" %% "spinalhdl-core" % spinalVersion
val spinalLib = "com.github.spinalhdl" %% "spinalhdl-lib" % spinalVersion
val spinalIdslPlugin = compilerPlugin("com.github.spinalhdl" %% "spinalhdl-idsl-plugin" % spinalVersion)
val circeCore = "io.circe" %% "circe-core" % "0.14.7"
val circeGeneric = "io.circe" %% "circe-generic" % "0.14.7"
val circeParser = "io.circe" %% "circe-parser" % "0.14.7"

lazy val projectname = (project in file("."))
  .settings(
    name := "AXISim", 
    Compile / scalaSource := baseDirectory.value / "hw" / "spinal",
    libraryDependencies ++= Seq(spinalCore, spinalLib, spinalIdslPlugin, circeCore, circeGeneric, circeParser)
  )

fork := true
