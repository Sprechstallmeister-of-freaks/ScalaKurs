package stats

class PartialClone1 {
  def processData(input: String): String = {
    val trimmed = input.trim
    val upper = trimmed.toUpperCase
    val result = upper.reverse
    result
  }
  
  def uniqueMethod(): Int = 42
}
