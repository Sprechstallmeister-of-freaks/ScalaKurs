package com.example

case class User(id: Long, name: String, age: Int)
case class Product(id: Long, title: String, price: Double)

class DataService {
  def getAdultUsers(users: List[User]): List[User] = {
    users.filter(_.age >= 18)
  }
  def getUserNames(users: List[User]): List[String] = {
    users.map(_.name)
  }
  def calculateTotal(products: List[Product]): Double = {
    products.map(_.price).sum
  }
  def getExpensiveProducts(products: List[Product], threshold: Double): List[Product] = {
    products.filter(_.price > threshold)
  }
}
