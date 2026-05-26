package com.plagiarism.algebras

import com.plagiarism.domain.DetectionResult

trait ReportGeneratorAlgebra[F[_]] {

  def generateHtmlReport(
                          result: DetectionResult,
                          outputPath: String
                        ): F[Either[Throwable, String]]

  def generateJsonReport(
                          result: DetectionResult,
                          outputPath: String
                        ): F[Either[Throwable, String]]

  def generateMarkdownReport(
                              result: DetectionResult,
                              outputPath: String
                            ): F[Either[Throwable, String]]

  def printSummary(result: DetectionResult): F[Unit]
}