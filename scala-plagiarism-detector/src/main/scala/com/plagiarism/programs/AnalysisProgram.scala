package com.plagiarism.programs

import cats.Monad
import cats.effect.std.Console
import cats.syntax.all._
import com.plagiarism.algebras.{PlagiarismDetectorAlgebra, ReportGeneratorAlgebra}
import com.plagiarism.domain.{DetectionConfig, DetectionResult, PlagiarismError}

class AnalysisProgram[F[_]: Monad: Console](
                                             detector: PlagiarismDetectorAlgebra[F],
                                             reportGen: ReportGeneratorAlgebra[F]
                                           ) {

  def run(
           sourcePath: String,
           jplagPath: String,
           config: DetectionConfig
         ): F[Unit] = {
    for {
      _ <- Console[F].println("Plagiarism Detection System for Scala Code")
      _ <- Console[F].println("=" * 60)
      _ <- Console[F].println(s"Source directory: $sourcePath")

      // Validate environment
      valid <- detector.validateEnvironment(jplagPath, sourcePath)
      _ <- valid match {
        case Left(err) => Console[F].println(s"Environment error: ${err.getMessage}")
        case Right(_) => Console[F].println("Environment OK")
      }

      // Run analysis
      result <- valid match {
        case Right(_) => detector.analyzeDirectory(sourcePath, config)
        case Left(err) => (err: PlagiarismError).asLeft[DetectionResult].pure[F]
      }

      // Process results
      _ <- result match {
        case Right(res) =>
          for {
            _ <- reportGen.printSummary(res)
            _ <- generateReports(res, config)
          } yield ()
        case Left(err) =>
          Console[F].println(s"Analysis failed: ${err.getMessage}")
      }
    } yield ()
  }

  private def generateReports(
                               result: DetectionResult,
                               config: DetectionConfig
                             ): F[Unit] = {
    for {
      htmlPath <- reportGen.generateHtmlReport(result, s"${config.outputDir}/report.html")
      _ <- htmlPath match {
        case Right(path) => Console[F].println(s"HTML report: $path")
        case Left(err) => Console[F].println(s"Failed to generate HTML: ${err.getMessage}")
      }

      jsonPath <- reportGen.generateJsonReport(result, s"${config.outputDir}/report.json")
      _ <- jsonPath match {
        case Right(path) => Console[F].println(s"JSON report: $path")
        case Left(err) => Console[F].println(s"Failed to generate JSON: ${err.getMessage}")
      }
    } yield ()
  }
}