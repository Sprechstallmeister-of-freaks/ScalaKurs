package stats

object SortAlgo1 {
  def bubbleSort(arr: Array[Int]): Array[Int] = {
    val n = arr.length
    for (i <- 0 until n-1) {
      for (j <- 0 until n-i-1) {
        if (arr(j) > arr(j+1)) {
          val temp = arr(j)
          arr(j) = arr(j+1)
          arr(j+1) = temp
        }
      }
    }
    arr
  }
}
