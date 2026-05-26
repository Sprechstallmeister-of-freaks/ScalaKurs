package com.example

import java.io.{File, PrintWriter}
import scala.io.Source

class FileHelper {
  def readFile(path: String): String = {
    Source.fromFile(path).mkString
  }
  def writeFile(path: String, content: String): Unit = {
    val writer = new PrintWriter(new File(path))
    try {
      writer.write(content)
    } finally {
      writer.close()
    }
  }
  def fileExists(path: String): Boolean = {
    new File(path).exists()
  }
  def getFileSize(path: String): Long = {
    new File(path).length()
  }
}
