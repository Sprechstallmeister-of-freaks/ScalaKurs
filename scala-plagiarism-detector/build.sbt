ThisBuild / scalaVersion := "2.13.16"
ThisBuild / version := "0.1.0"
ThisBuild / organization := "com.plagiarism"

lazy val root = (project in file("."))
  .settings(
    name := "scala-plagiarism-detector",
    libraryDependencies ++= Seq(
      "org.typelevel" %% "cats-effect" % "3.5.4",
      "io.circe" %% "circe-parser" % "0.14.7"
    ),
    Compile / unmanagedJars := {
      val libDir = baseDirectory.value / "lib"
      if (libDir.exists()) (libDir ** "*.jar").classpath
      else Seq.empty
    },
    // Отключаем предупреждения
    scalacOptions := Seq("-deprecation", "-feature"),
    // Явно указываем использование Java 17
    javacOptions ++= Seq("-source", "17", "-target", "17"),
    // Используем форк для запуска
    fork := true
  )