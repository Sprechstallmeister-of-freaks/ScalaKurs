package com.plagiarism.interpreters

import cats.effect.Sync
import cats.effect.std.Console
import cats.syntax.all._
import com.plagiarism.algebras.ReportGeneratorAlgebra
import com.plagiarism.domain.{CloneMatch, DetectionResult}
import java.io.{File, PrintWriter}
import java.time.format.DateTimeFormatter
import java.time.Instant

class ConsoleReportGenerator[F[_]: Sync: Console]
  extends ReportGeneratorAlgebra[F] {

  override def generateHtmlReport(
                                   result: DetectionResult,
                                   outputPath: String
                                 ): F[Either[Throwable, String]] = Sync[F].delay {
    try {
      val html = createHtmlReport(result)
      writeToFile(outputPath, html)
      Right(outputPath)
    } catch {
      case e: Throwable => Left(e)
    }
  }

  override def generateJsonReport(
                                   result: DetectionResult,
                                   outputPath: String
                                 ): F[Either[Throwable, String]] = Sync[F].delay {
    try {
      val json = createJsonReport(result)
      writeToFile(outputPath, json)
      Right(outputPath)
    } catch {
      case e: Throwable => Left(e)
    }
  }

  override def generateMarkdownReport(
                                       result: DetectionResult,
                                       outputPath: String
                                     ): F[Either[Throwable, String]] = Sync[F].delay {
    try {
      val md = createMarkdownReport(result)
      writeToFile(outputPath, md)
      Right(outputPath)
    } catch {
      case e: Throwable => Left(e)
    }
  }

  override def printSummary(result: DetectionResult): F[Unit] = {
    for {
      _ <- Console[F].println("\n" + "=" * 60)
      _ <- Console[F].println("DETECTION RESULTS")
      _ <- Console[F].println("=" * 60)
      _ <- Console[F].println(s"Files analyzed: ${result.totalFiles}")
      _ <- Console[F].println(s"Clone pairs found: ${result.totalMatches}")
      _ <- Console[F].println(s"Average similarity: ${result.averageSimilarity.round}%")
      _ <- printCloneTable(result.matches)
      _ <- printStatistics(result.matches)
    } yield ()
  }

  private def printCloneTable(matches: List[CloneMatch]): F[Unit] = {
    if (matches.isEmpty) {
      Console[F].println("\nNo clones detected!")
    } else {
      for {
        _ <- Console[F].println("\nCLONE PAIRS:")
        _ <- Console[F].println("   " + "-" * 70)
        _ <- matches.zipWithIndex.traverse_ { case (m, i) =>
          val name1 = m.sourceFile.replace(".scala", "")
          val name2 = m.targetFile.replace(".scala", "")
          Console[F].println(f"   ${i + 1}%2d. $name1%-30s <-> $name2%-30s")
          Console[F].println(f"       Similarity: ${m.similarity}%.1f%%   Type: ${m.cloneType}")
        }
      } yield ()
    }
  }

  private def printStatistics(matches: List[CloneMatch]): F[Unit] = {
    val t1Count = matches.count(_.cloneType.contains("T1"))
    val t2Count = matches.count(_.cloneType.contains("T2"))
    val t3Count = matches.count(_.cloneType.contains("T3"))

    for {
      _ <- Console[F].println("\n" + "=" * 60)
      _ <- Console[F].println("CLONE TYPE STATISTICS")
      _ <- Console[F].println("=" * 60)
      _ <- Console[F].println(s"   Type T1 (formatting only): $t1Count")
      _ <- Console[F].println(s"   Type T2 (renamed identifiers): $t2Count")
      _ <- Console[F].println(s"   Type T3 (semantic/structure): $t3Count")
    } yield ()
  }

  private def createHtmlReport(result: DetectionResult): String = {
    import java.text.SimpleDateFormat
    import java.util.Date
    val dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
    val date = dateFormat.format(new Date(result.timestamp))

    val rows = result.matches.zipWithIndex.map { case (m, i) =>
      s"""<tr>
         |   <td>${i + 1}</td>
         |   <td>${m.sourceFile}</td>
         |   <td>${m.targetFile}</td>
         |   <td>${m.similarity.round}%</td>
         |   <td>${m.cloneType}</td>
         |</tr>""".stripMargin
    }.mkString("\n")

    s"""<!DOCTYPE html>
       |<html>
       |<head><title>Plagiarism Report</title>
       |<style>
       |  body { font-family: Arial; margin: 20px; }
       |  table { border-collapse: collapse; width: 100%%; }
       |  th, td { border: 1px solid #ddd; padding: 8px; text-align: left; }
       |  th { background-color: #4CAF50; color: white; }
       |</style>
       |</head>
       |<body>
       |  <h1>Plagiarism Detection Report</h1>
       |  <p>Date: $date</p>
       |  <p>Files: ${result.totalFiles} | Matches: ${result.totalMatches} | Avg Similarity: ${result.averageSimilarity.round}%</p>
       |  <table>
       |    <tr><th>#</th><th>Source</th><th>Target</th><th>Similarity</th><th>Type</th></tr>
       |    $rows
       |  </table>
       |</body>
       |</html>""".stripMargin
  }

  private def createJsonReport(result: DetectionResult): String = {
    s"""{
       |  "timestamp": ${result.timestamp},
       |  "totalFiles": ${result.totalFiles},
       |  "totalMatches": ${result.totalMatches},
       |  "averageSimilarity": ${result.averageSimilarity},
       |  "matches": [
       |    ${result.matches.map(m =>
      s"""{"source": "${m.sourceFile}", "target": "${m.targetFile}", "similarity": ${m.similarity}, "type": "${m.cloneType}"}"""
    ).mkString(",\n    ")}
       |  ]
       |}""".stripMargin
  }

  private def createMarkdownReport(result: DetectionResult): String = {
    val table = result.matches.zipWithIndex.map { case (m, i) =>
      s"| ${i + 1} | ${m.sourceFile} | ${m.targetFile} | ${m.similarity.round}% | ${m.cloneType} |"
    }.mkString("\n")

    s"""# Plagiarism Detection Report
       |
       |## Summary
       |- **Files analyzed:** ${result.totalFiles}
       |- **Clone pairs found:** ${result.totalMatches}
       |- **Average similarity:** ${result.averageSimilarity.round}%
       |
       |## Clone Pairs
       || # | Source | Target | Similarity | Type |
       ||---|--------|--------|------------|------|
       |$table
       |""".stripMargin
  }

  private def writeToFile(path: String, content: String): Unit = {
    val writer = new PrintWriter(new File(path))
    try writer.write(content) finally writer.close()
  }
}