package stats

class ComplexClone2 {
  def computeMean(values: List[Double]): Double = {
    if (values.isEmpty) 0.0
    else values.sum / values.length
  }
  
  def computeMiddle(values: List[Double]): Double = {
    val sortedValues = values.sorted
    val count = sortedValues.size
    if (count % 2 == 1) sortedValues(count / 2)
    else (sortedValues(count / 2 - 1) + sortedValues(count / 2)) / 2.0
  }
  
  def computeSpread(values: List[Double]): Double = {
    val meanValue = computeMean(values)
    values.map(v => Math.pow(v - meanValue, 2)).sum / values.length
  }
}
