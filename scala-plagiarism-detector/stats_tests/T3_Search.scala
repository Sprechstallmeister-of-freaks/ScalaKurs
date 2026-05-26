package stats

object SearchAlgo1 {
  def linearSearch(data: List[Int], target: Int): Int = {
    var index = -1
    for (i <- data.indices) {
      if (data(i) == target) {
        index = i
      }
    }
    index
  }
}
