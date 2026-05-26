package com.plagiarism.domain

sealed trait PlagiarismError extends Throwable

object PlagiarismError {
  case class JPlagNotFound(path: String) extends PlagiarismError
  case class JPlagExecutionFailed(exitCode: Int, message: String) extends PlagiarismError
  case class InvalidInputPath(path: String) extends PlagiarismError
  case class ParsingError(message: String) extends PlagiarismError
  case class ConfigurationError(message: String) extends PlagiarismError  // Добавлено
}

case class CloneMatch(
                       sourceFile: String,
                       targetFile: String,
                       similarity: Double,
                       cloneType: String
                     )

case class DetectionConfig(
                            minTokens: Int = 9,
                            language: String = "scala",
                            outputDir: String = "reports",
                            sensitivity: Double = 30.0
                          )

case class DetectionResult(
                            timestamp: Long,
                            totalFiles: Int,
                            totalMatches: Int,
                            matches: List[CloneMatch],
                            averageSimilarity: Double
                          )