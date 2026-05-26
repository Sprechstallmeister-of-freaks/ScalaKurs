package com.example

object StringUtils {
  def reverse(s: String): String = s.reverse
  def toUpperCase(s: String): String = s.toUpperCase
  def toLowerCase(s: String): String = s.toLowerCase
  def capitalize(s: String): String = {
    if (s.isEmpty) s
    else s.substring(0, 1).toUpperCase + s.substring(1).toLowerCase
  }
  def isPalindrome(s: String): Boolean = s == s.reverse
}
