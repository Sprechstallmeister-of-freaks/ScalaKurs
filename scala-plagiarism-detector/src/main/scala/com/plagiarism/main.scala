package com.plagiarism

import cats.effect.{IO, IOApp}
import com.plagiarism.api.ApiServer

object Main extends IOApp.Simple {

  override def run: IO[Unit] = {
    println("=" * 50)
    println("Plagiarism Detection API Server")
    println("=" * 50)
    println("Starting server...")
    println("Health check: http://localhost:8080/api/health")
    println("Analyze GET: http://localhost:8080/api/analyze/stats_tests")
    println("=" * 50)
    ApiServer.run("0.0.0.0", 8080)
  }
}