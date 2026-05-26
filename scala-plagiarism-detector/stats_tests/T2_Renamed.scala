package stats

class DataStore2 {
  def savePerson(uid: Int, fullName: String): Boolean = {
    println(s"Saving person: $fullName")
    true
  }
  def removePerson(uid: Int): Boolean = {
    println(s"Removing person: $uid")
    true
  }
}
