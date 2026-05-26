ThisBuild / scalaVersion := "2.13.16"
ThisBuild / version := "0.1.0"
ThisBuild / organization := "com.plagiarism"

lazy val root = (project in file("."))
  .settings(
    name := "scala-plagiarism-detector",

    libraryDependencies ++= Seq(
      // Cats Effect 3
      "org.typelevel" %% "cats-effect" % "3.5.4",

      // Cats Tagless Final (правильная версия)
      "org.typelevel" %% "cats-tagless-macros" % "0.16.1",
      "org.typelevel" %% "cats-tagless-core" % "0.16.1",

      // Circe для JSON
      "io.circe" %% "circe-core" % "0.14.7",
      "io.circe" %% "circe-generic" % "0.14.7",
      "io.circe" %% "circe-parser" % "0.14.7"
    ),

    // Подключаем JPlag JAR
    Compile / unmanagedJars := {
      val libDir = baseDirectory.value / "lib"
      if (libDir.exists()) (libDir ** "*.jar").classpath
      else Seq.empty
    },

    fork := true,

    scalacOptions ++= Seq(
      "-language:higherKinds",
      "-Ymacro-annotations",
      "-deprecation"
    ),

    javaOptions ++= Seq("-Xmx2g")
  )

// Авто-скачивание JPlag
lazy val downloadJPlag = taskKey[Unit]("Download JPlag JAR")
downloadJPlag := {
  import java.net.URL
  import java.nio.file.{Files, Paths, StandardCopyOption}

  val libDir = baseDirectory.value / "lib"
  if (!libDir.exists()) libDir.mkdirs()

  val jarFile = libDir / "jplag.jar"
  if (!jarFile.exists()) {
    println("Downloading JPlag 4.0.0...")
    val url = new URL("https://github.com/jplag/JPlag/releases/download/v4.0.0/jplag-4.0.0-jar-with-dependencies.jar")
    Files.copy(url.openStream(), Paths.get(jarFile.path), StandardCopyOption.REPLACE_EXISTING)
    println(s"Downloaded to ${jarFile.path}")
  }
}

Compile / compile := (Compile / compile).dependsOn(downloadJPlag).value