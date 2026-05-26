package stats

class DataStore1 {
  def saveUser(id: Int, name: String): Boolean = {
    println(s"Saving user: $name")
    true
  }
  def deleteUser(id: Int): Boolean = {
    println(s"Deleting user: $id")
    true
  }
}
