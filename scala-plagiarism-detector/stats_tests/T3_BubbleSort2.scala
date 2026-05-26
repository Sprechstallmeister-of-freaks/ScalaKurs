package stats

object SortAlgo2 {
  def sort(numbers: Array[Int]): Array[Int] = {
    val length = numbers.length
    for (outer <- 0 until length-1) {
      for (inner <- 0 until length-outer-1) {
        if (numbers(inner) > numbers(inner+1)) {
          val temporary = numbers(inner)
          numbers(inner) = numbers(inner+1)
          numbers(inner+1) = temporary
        }
      }
    }
    numbers
  }
}
