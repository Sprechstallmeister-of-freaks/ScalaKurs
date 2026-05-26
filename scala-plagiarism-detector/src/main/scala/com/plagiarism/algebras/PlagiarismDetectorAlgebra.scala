package com.plagiarism.algebras

import com.plagiarism.domain._

trait PlagiarismDetectorAlgebra[F[_]] {

  def analyzeDirectory(
                        path: String,
                        config: DetectionConfig
                      ): F[Either[PlagiarismError, DetectionResult]]

  def compareFiles(
                    file1: String,
                    file2: String,
                    config: DetectionConfig
                  ): F[Either[PlagiarismError, CloneMatch]]

  def validateEnvironment(
                           jplagPath: String,
                           sourcePath: String
                         ): F[Either[PlagiarismError, Unit]]

  def getVersion(jplagPath: String): F[Either[PlagiarismError, String]]
}