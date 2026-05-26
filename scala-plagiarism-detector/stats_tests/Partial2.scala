package stats

class PartialClone2 {
  def processInput(data: String): String = {
    val trimmed = data.trim
    val lower = trimmed.toLowerCase
    val reversed = trimmed.reverse
    reversed
  }
  
  def differentMethod(): String = "hello"
}
