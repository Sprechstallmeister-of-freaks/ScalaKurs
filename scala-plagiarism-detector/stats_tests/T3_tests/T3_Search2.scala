package stats

object SearchAlgo2 {
  def findElement(list: List[Int], value: Int): Int = {
    var position = -1
    for (idx <- list.indices) {
      if (list(idx) == value) {
        position = idx
      }
    }
    position
  }
}
