package stats

object Unique2 {
  def factorial(n: Int): Int = {
    if (n == 0) 1
    else n * factorial(n-1)
  }
}
