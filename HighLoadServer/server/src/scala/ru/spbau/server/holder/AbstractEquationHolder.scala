package ru.spbau.server.holder

import java.nio.ByteBuffer

/**
 * User: nikita_kartashov
 * Date: 25.12.2014
 * Time: 16:54
 */
trait AbstractEquationHolder {

  case class Equation(byteBuffer: ByteBuffer, offsetInts: Int) {
    def a = byteBuffer.asIntBuffer().get(0 + offsetInts)

    def b = byteBuffer.asIntBuffer().get(1 + offsetInts)

    def c = byteBuffer.asIntBuffer().get(2 + offsetInts)

    def solve = {
      val d = b * b - 4 * a * c
      val result = d match {
        case _ if d > 0 => 2
        case _ if d == 0 => 1
        case _ if d < 0 => 0
      }
      fact(result + 500)
    }

    def fact(i : Int) = {
      (1 to i).product
    }
  }

  def apply(i: Int): Equation

  def getEquationsNumber: Int
}
