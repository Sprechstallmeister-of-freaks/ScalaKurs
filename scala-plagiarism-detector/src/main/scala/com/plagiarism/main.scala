package com.plagiarism

import cats.effect.{IO, IOApp}
import com.plagiarism.domain.DetectionConfig
import com.plagiarism.interpreters.{ConsoleReportGenerator, JPlagDetectorInterpreter}
import com.plagiarism.programs.AnalysisProgram

object Main extends IOApp.Simple {

  private val JPlagPath = "lib/jplag.jar"
  private val SourcePath = "./stats_tests"

  override def run: IO[Unit] = {
    // Передаём jplagPath в конструктор
    val detector = new JPlagDetectorInterpreter[IO](JPlagPath)
    val reportGen = new ConsoleReportGenerator[IO]
    val program = new AnalysisProgram[IO](detector, reportGen)

    program.run(
      sourcePath = SourcePath,
      jplagPath = JPlagPath,
      config = DetectionConfig()
    )
  }
}