package com.example

case class Person(id: Long, fullName: String, years: Int)
case class Item(id: Long, name: String, cost: Double)

class DataProcessor {
  def getAdultPersons(persons: List[Person]): List[Person] = {
    persons.filter(_.years >= 18)
  }
  def getPersonFullNames(persons: List[Person]): List[String] = {
    persons.map(_.fullName)
  }
  def calculateTotalCost(items: List[Item]): Double = {
    items.map(_.cost).sum
  }
  def getCostlyItems(items: List[Item], minCost: Double): List[Item] = {
    items.filter(_.cost > minCost)
  }
}
