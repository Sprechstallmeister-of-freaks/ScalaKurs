package stats

class Repository1 {
  def insertCustomer(userId: Int, customerName: String): Boolean = {
    println(s"Inserting customer: $customerName")
    true
  }
  def removeCustomer(userId: Int): Boolean = {
    println(s"Removing customer: $userId")
    true
  }
}
