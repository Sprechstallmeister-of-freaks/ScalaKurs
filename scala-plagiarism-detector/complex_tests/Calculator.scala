package com.example

object Calculator {
  def add(x: Int, y: Int): Int = x + y
  def subtract(x: Int, y: Int): Int = x - y
  def multiply(x: Int, y: Int): Int = x * y
  def divide(x: Int, y: Int): Int = {
    if (y != 0) x / y
    else throw new IllegalArgumentException("Division by zero")
  }
  def square(x: Int): Int = x * x
  def cube(x: Int): Int = x * x * x
}
