scalaVersion := "3.3.7"
version      := "0.1.0-SNAPSHOT"

// JavaFX ships native binaries per-OS; pick the right classifier for
// whichever machine is running sbt.
lazy val javaFXOsName = System.getProperty("os.name").toLowerCase match {
  case n if n.contains("mac") => "mac"
  case n if n.contains("win") => "win"
  case n if n.contains("linux") => "linux"
  case _ => throw new Exception("Unknown platform")
}

lazy val root = (project in file("."))
  .settings(
    name := "Subsidy Eligibility Calculator",

    // run app in separate JVM process, not inside sbt's own JVM
    fork := true,

    scalacOptions ++= Seq("-Wunused:all", "-Wconf:cat=deprecation:s"),

    // makes ScalaFX's scala-friendly wrappers - onAction = handle {}, <== available
    libraryDependencies += "org.scalafx" %% "scalafx" % "21.0.0-R32",

    // 4 separate JavaFX modules pulled, splitting into individual pieces
    libraryDependencies ++= Seq("base", "controls", "fxml", "graphics")
      .map(m => "org.openjfx" % s"javafx-$m" % "25.0.3" classifier javaFXOsName),

    // sbt-assembly is optional (from project/plugins.sbt)
    // allows to build a single runnable fat jar with `sbt assembly`.
    assembly / assemblyMergeStrategy := {
      case PathList("META-INF", _*) => MergeStrategy.discard //drop it, no merging/keeping it
      case _                        => MergeStrategy.first   // keep whichever copy was found first
    },
    assembly / mainClass := Some("Main")
  )
