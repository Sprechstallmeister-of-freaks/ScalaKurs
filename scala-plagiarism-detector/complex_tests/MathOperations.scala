package com.example

object MathOperations {
  def sum(a: Int, b: Int): Int = a + b
  def difference(a: Int, b: Int): Int = a - b
  def product(a: Int, b: Int): Int = a * b
  def quotient(a: Int, b: Int): Int = {
    if (b != 0) a / b
    else throw new ArithmeticException("Division by zero")
  }
  def squared(a: Int): Int = a * a
  def cubed(a: Int): Int = a * a * a
}
