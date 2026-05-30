package com.plagiarism.interpreters

import cats.effect.{Async, Sync}
import cats.effect.std.Console
import cats.syntax.all._
import com.plagiarism.algebras.PlagiarismDetectorAlgebra
import com.plagiarism.domain._
import com.plagiarism.domain.PlagiarismError._
import scala.sys.process._
import java.io.{ByteArrayOutputStream, File}
import java.nio.file.{Files, Paths, StandardCopyOption}

class JPlagDetectorInterpreter[F[_]: Async: Console](
                                                      private val jplagPath: String
                                                    ) extends PlagiarismDetectorAlgebra[F] {

  override def analyzeDirectory(
                                 path: String,
                                 config: DetectionConfig
                               ): F[Either[PlagiarismError, DetectionResult]] = {
    for {
      _ <- Console[F].println(s"Scanning root directory: $path")

      // 1. Находим все .scala файлы рекурсивно
      allFiles <- findAllScalaFiles(path)
      _ <- Console[F].println(s"Found ${allFiles.size} Scala files in all subdirectories")

      // 2. Если файлов нет - возвращаем пустой результат
      result <- if (allFiles.isEmpty) {
        createEmptyResult(path).map(Right.apply)
      } else {
        // 3. Создаём временную директорию и копируем все файлы
        for {
          tempDir <- createTempDir
          _ <- copyAllFilesToTemp(allFiles, tempDir)
          _ <- Console[F].println(s"Copied all files to temporary directory: ${tempDir.getAbsolutePath}")

          // 4. Запускаем JPlag на временной директории
          outputEither <- executeJPlag(tempDir.getAbsolutePath, config)
          parsed <- outputEither match {
            case Right(output) => parseOutput(output, allFiles.size, config)
            case Left(err) => (err: PlagiarismError).asLeft[DetectionResult].pure[F]
          }
        } yield parsed
      }
    } yield result
  }

  override def compareFiles(
                             file1: String,
                             file2: String,
                             config: DetectionConfig
                           ): F[Either[PlagiarismError, CloneMatch]] = {
    for {
      tempDir <- createTempDir
      _ <- copyToTemp(file1, tempDir)
      _ <- copyToTemp(file2, tempDir)
      allFiles <- findAllScalaFiles(tempDir.getAbsolutePath)
      result <- analyzeDirectory(tempDir.getAbsolutePath, config)
      matchResult <- result match {
        case Right(detectionResult) =>
          detectionResult.matches.headOption match {
            case Some(m) => m.asRight[PlagiarismError].pure[F]
            case None => ParsingError("No matches found").asLeft[CloneMatch].pure[F]
          }
        case Left(err) => err.asLeft[CloneMatch].pure[F]
      }
    } yield matchResult
  }

  override def validateEnvironment(
                                    jplagPathParam: String,
                                    sourcePath: String
                                  ): F[Either[PlagiarismError, Unit]] = {
    for {
      jarExists <- fileExists(jplagPathParam)
      dirExists <- directoryExists(sourcePath)
      javaExists <- checkJavaAvailable
      _ <- Console[F].println(s"JPlag JAR: ${if(jarExists) "OK" else "MISSING"} ($jplagPathParam)")
      _ <- Console[F].println(s"Source dir: ${if(dirExists) "OK" else "MISSING"} ($sourcePath)")
      _ <- Console[F].println(s"Java: ${if(javaExists) "OK" else "MISSING"}")
    } yield {
      if (!jarExists) Left(JPlagNotFound(jplagPathParam))
      else if (!dirExists) Left(InvalidInputPath(sourcePath))
      else if (!javaExists) Left(ConfigurationError("Java not found in PATH"))
      else Right(())
    }
  }

  override def getVersion(jplagPathParam: String): F[Either[PlagiarismError, String]] = Sync[F].delay {
    try {
      val version = Seq("java", "-jar", jplagPathParam, "--version").!!
      Right(version.trim)
    } catch {
      case _: Exception => Left(JPlagNotFound(jplagPathParam))
    }
  }

  // ============================================
  // Рекурсивный поиск всех .scala файлов (иммутабельно)
  // ============================================

  private def findAllScalaFiles(rootPath: String): F[List[File]] = Sync[F].delay {
    val rootDir = new File(rootPath)
    if (!rootDir.exists() || !rootDir.isDirectory) {
      List.empty
    } else {
      findAllScalaFilesRecursive(rootDir)
    }
  }

  private def findAllScalaFilesRecursive(dir: File): List[File] = {
    // Файлы .scala в текущей директории
    val scalaFiles = Option(dir.listFiles())
      .getOrElse(Array.empty)
      .filter(f => f.isFile && f.getName.endsWith(".scala"))
      .toList

    // Рекурсивно обходим поддиректории
    val subDirs = Option(dir.listFiles())
      .getOrElse(Array.empty)
      .filter(_.isDirectory)
      .toList

    val filesFromSubDirs = subDirs.flatMap(findAllScalaFilesRecursive)

    scalaFiles ++ filesFromSubDirs
  }

  // ============================================
  // Копирование всех файлов во временную папку
  // ============================================

  private def copyAllFilesToTemp(files: List[File], tempDir: File): F[Unit] = {
    files.traverse_(file => copyToTemp(file.getAbsolutePath, tempDir))
  }

  // ============================================
  // Остальные вспомогательные методы
  // ============================================

  private def validatePath(path: String): F[Either[PlagiarismError, String]] = Sync[F].delay {
    val file = new File(path)
    if (!file.exists()) Left(InvalidInputPath(path))
    else if (!file.isDirectory) Left(InvalidInputPath(s"$path is not a directory"))
    else Right(file.getAbsolutePath)
  }

  private def executeJPlag(dir: String, config: DetectionConfig): F[Either[PlagiarismError, String]] = Sync[F].delay {
    val outputDir = s"${config.outputDir}/jplag_${System.currentTimeMillis()}"
    new File(outputDir).mkdirs()

    val command = Seq(
      "java", "-jar", jplagPath,
      "-l", config.language,
      "-t", config.minTokens.toString,
      "-r", outputDir,
      dir
    )

    val output = new ByteArrayOutputStream()
    val exitCode = command ! ProcessLogger(
      line => output.write((line + "\n").getBytes),
      _ => ()
    )

    if (exitCode == 0) Right(output.toString)
    else Left(JPlagExecutionFailed(exitCode, output.toString))
  }

  // ПОЛНОСТЬЮ ИММУТАБЕЛЬНАЯ ВЕРСИЯ parseOutput
  private def parseOutput(output: String, totalFiles: Int, config: DetectionConfig): F[Either[PlagiarismError, DetectionResult]] = Sync[F].delay {
    val pattern = """Comparing ([^\s-]+)-([^\s-]+)\.scala: ([\d.]+)""".r

    // Иммутабельно: используем flatMap + map вместо ListBuffer
    val matches = pattern.findAllMatchIn(output).toList.flatMap { m =>
      val similarity = m.group(3).toDouble * 100
      val file1 = s"${m.group(1)}.scala"
      val file2 = s"${m.group(2)}.scala"

      if (similarity >= config.sensitivity && file1 != file2) {
        Some(CloneMatch(file1, file2, similarity, detectType(similarity)))
      } else None
    }

    // Иммутабельное удаление дубликатов (A->B и B->A)
    val uniqueMatches = matches
      .groupBy(m => Set(m.sourceFile, m.targetFile))
      .values
      .map(_.head)
      .toList

    val result = DetectionResult(
      timestamp = System.currentTimeMillis(),
      totalFiles = totalFiles,
      totalMatches = uniqueMatches.size,
      matches = uniqueMatches,
      averageSimilarity = if (uniqueMatches.nonEmpty)
        uniqueMatches.map(_.similarity).sum / uniqueMatches.size
      else 0.0
    )

    Right(result)
  }

  private def createEmptyResult(rootPath: String): F[DetectionResult] = Sync[F].delay {
    DetectionResult(
      timestamp = System.currentTimeMillis(),
      totalFiles = 0,
      totalMatches = 0,
      matches = List.empty,
      averageSimilarity = 0.0
    )
  }

  private def detectType(similarity: Double): String = {
    if (similarity >= 99) "T1/T2 (Structural clone)"
    else if (similarity >= 70) "T2 (Renamed identifiers)"
    else if (similarity >= 30) "T3 (Semantic clone)"
    else "Partial match"
  }

  private def fileExists(path: String): F[Boolean] = Sync[F].delay(new File(path).exists())

  private def directoryExists(path: String): F[Boolean] = Sync[F].delay {
    val d = new File(path); d.exists() && d.isDirectory
  }

  private def checkJavaAvailable: F[Boolean] = Sync[F].delay {
    try {
      "java -version".!!
      true
    } catch {
      case _: Exception => false
    }
  }

  private def createTempDir: F[File] = Sync[F].delay {
    val tempDir = Files.createTempDirectory("jplag_analysis_").toFile
    tempDir.deleteOnExit()
    tempDir
  }

  private def copyToTemp(sourcePath: String, destDir: File): F[Unit] = Sync[F].delay {
    val source = Paths.get(sourcePath)
    val target = destDir.toPath.resolve(source.getFileName)
    Files.copy(source, target, StandardCopyOption.REPLACE_EXISTING)
    ()
  }
}