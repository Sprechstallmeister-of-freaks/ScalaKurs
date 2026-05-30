ThisBuild / scalaVersion := "2.13.16"
ThisBuild / version := "0.1.0"
ThisBuild / organization := "com.plagiarism"

lazy val root = (project in file("."))
  .settings(
    name := "scala-plagiarism-detector",

    libraryDependencies ++= Seq(
      // Cats Effect 3
      "org.typelevel" %% "cats-effect" % "3.5.4",
      "org.typelevel" %% "cats-core" % "2.10.0",

      // Http4s для API (правильные версии)
      "org.http4s" %% "http4s-ember-server" % "0.23.27",
      "org.http4s" %% "http4s-ember-client" % "0.23.27",
      "org.http4s" %% "http4s-circe" % "0.23.27",
      "org.http4s" %% "http4s-dsl" % "0.23.27",

      // Circe для JSON
      "io.circe" %% "circe-core" % "0.14.7",
      "io.circe" %% "circe-generic" % "0.14.7",
      "io.circe" %% "circe-parser" % "0.14.7",

      // Logging
      "ch.qos.logback" % "logback-classic" % "1.4.14",
      "org.typelevel" %% "log4cats-slf4j" % "2.6.0"
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

    // Добавляем резервные репозитории
    resolvers ++= Seq(
      "Maven Central" at "https://repo1.maven.org/maven2/",
      "Sonatype Releases" at "https://oss.sonatype.org/content/repositories/releases/"
    )
  )