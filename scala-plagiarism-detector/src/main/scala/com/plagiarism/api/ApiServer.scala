package com.plagiarism.api

import cats.effect.{IO, SyncIO}
import cats.effect.unsafe.implicits.global
import com.plagiarism.domain._
import com.plagiarism.interpreters.{ConsoleReportGenerator, JPlagDetectorInterpreter}
import com.plagiarism.programs.AnalysisProgram
import com.sun.net.httpserver.{HttpExchange, HttpServer}
import java.net.InetSocketAddress
import java.nio.charset.StandardCharsets

class ApiServer(
                 detector: JPlagDetectorInterpreter[IO],
                 reportGen: ConsoleReportGenerator[IO],
                 host: String,
                 port: Int
               ) {

  private val program = new AnalysisProgram[IO](detector, reportGen)

  private def sendResponse(exchange: HttpExchange, statusCode: Int, response: String): Unit = {
    val bytes = response.getBytes(StandardCharsets.UTF_8)
    exchange.sendResponseHeaders(statusCode, bytes.length)
    val os = exchange.getResponseBody
    os.write(bytes)
    os.close()
  }

  private def handleHealth(exchange: HttpExchange): Unit = {
    val response = s"""{"status":"OK","version":"1.0.0","timestamp":${System.currentTimeMillis()}}"""
    exchange.getResponseHeaders.set("Content-Type", "application/json")
    sendResponse(exchange, 200, response)
  }

  private def handleAnalyze(exchange: HttpExchange, path: String): Unit = {
    val params = if (exchange.getRequestMethod == "POST") {
      val body = scala.io.Source.fromInputStream(exchange.getRequestBody).mkString
      parseJsonParams(body)
    } else {
      Map("sourcePath" -> path)
    }

    val sourcePath = params.getOrElse("sourcePath",
      if (path.nonEmpty && path != "analyze") path else "./stats_tests")
    val jplagPath = params.getOrElse("jplagPath", "lib/jplag.jar")
    val minTokens = params.get("minTokens").map(_.toInt).getOrElse(9)
    val language = params.getOrElse("language", "scala")
    val sensitivity = params.get("sensitivity").map(_.toDouble).getOrElse(30.0)

    val config = DetectionConfig(minTokens, language, "reports", sensitivity)

    val resultEither = program.runWithResult(sourcePath, jplagPath, config).unsafeRunSync()

    val response = resultEither match {
      case Right(detectionResult: DetectionResult) =>
        s"""{"success":true,"message":"Found ${detectionResult.totalMatches} clone pairs","result":${toJson(detectionResult)},"error":null}"""
      case Left(err: PlagiarismError) =>
        s"""{"success":false,"message":"Analysis failed","result":null,"error":"${err.getMessage}"}"""
    }

    exchange.getResponseHeaders.set("Content-Type", "application/json")
    sendResponse(exchange, 200, response)
  }

  private def parseJsonParams(body: String): Map[String, String] = {
    try {
      body.replaceAll("[{}\"]", "")
        .split(",")
        .flatMap { pair =>
          pair.split(":") match {
            case Array(k, v) => Some(k.trim -> v.trim)
            case _ => None
          }
        }.toMap
    } catch {
      case _: Exception => Map.empty
    }
  }

  private def toJson(result: DetectionResult): String = {
    val matchesJson = result.matches.map { m =>
      s"""{"sourceFile":"${m.sourceFile}","targetFile":"${m.targetFile}","similarity":${m.similarity},"cloneType":"${m.cloneType}"}"""
    }.mkString("[", ",", "]")

    s"""{"timestamp":${result.timestamp},"totalFiles":${result.totalFiles},"totalMatches":${result.totalMatches},"averageSimilarity":${result.averageSimilarity},"matches":$matchesJson}"""
  }

  private def handleRequest(exchange: HttpExchange): Unit = {
    val method = exchange.getRequestMethod
    val path = exchange.getRequestURI.getPath

    try {
      (method, path) match {
        case ("GET", "/api/health") => handleHealth(exchange)
        case ("GET", p) if p.startsWith("/api/analyze/") =>
          val dir = p.substring("/api/analyze/".length)
          handleAnalyze(exchange, dir)
        case ("POST", "/api/analyze") => handleAnalyze(exchange, "")
        case _ =>
          val response = """{"error":"Not found"}"""
          sendResponse(exchange, 404, response)
      }
    } catch {
      case e: Exception =>
        val response = s"""{"error":"${e.getMessage}"}"""
        sendResponse(exchange, 500, response)
    }
  }

  def start(): IO[Unit] = IO {
    val server = HttpServer.create(new InetSocketAddress(host, port), 0)
    server.createContext("/", (exchange: HttpExchange) => handleRequest(exchange))
    server.setExecutor(null)
    server.start()
    println(s"API Server started on http://$host:$port")
    println(s"Health check: http://localhost:$port/api/health")
    println(s"Analyze GET: http://localhost:$port/api/analyze/stats_tests")
    println(s"Press Ctrl+C to stop")
  }.void

  def waitForShutdown(): IO[Unit] = IO {
    println("Server running. Press ENTER to stop...")
    scala.io.StdIn.readLine()
  }.void
}

object ApiServer {
  def run(host: String = "0.0.0.0", port: Int = 8080): IO[Unit] = {
    val detector = new JPlagDetectorInterpreter[IO]("lib/jplag.jar")
    val reportGen = new ConsoleReportGenerator[IO]
    val server = new ApiServer(detector, reportGen, host, port)

    for {
      _ <- server.start()
      _ <- server.waitForShutdown()
    } yield ()
  }
}