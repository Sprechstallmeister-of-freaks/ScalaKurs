package stats

class ComplexClone1 {
  def calculateAverage(numbers: List[Double]): Double = {
    if (numbers.isEmpty) 0.0
    else numbers.sum / numbers.length
  }
  
  def calculateMedian(numbers: List[Double]): Double = {
    val sorted = numbers.sorted
    val size = sorted.size
    if (size % 2 == 1) sorted(size / 2)
    else (sorted(size / 2 - 1) + sorted(size / 2)) / 2.0
  }
  
  def calculateVariance(numbers: List[Double]): Double = {
    val avg = calculateAverage(numbers)
    numbers.map(x => Math.pow(x - avg, 2)).sum / numbers.length
  }
}
