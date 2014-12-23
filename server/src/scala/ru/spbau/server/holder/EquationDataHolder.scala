package ru.spbau.server.holder

import java.nio.ByteBuffer

/**
 * User: nikita_kartashov
 * Date: 20.12.2014
 * Time: 17:51
 */
class EquationDataHolder extends AbstractDataHolder {
  override def isFinished: Boolean = bytesTillFinished == 0

  override def write(newBytes: ByteBuffer, length: Int): Unit = {
    (0 until length).foreach(i => bytes.put(i, newBytes.get(i)))
    currentIndex += length
  }

  case class Equation(byteBuffer: ByteBuffer, offset: Int) {
    def a = byteBuffer.asIntBuffer().get(0 + offset)
    def b = byteBuffer.asIntBuffer().get(1 + offset)
    def c = byteBuffer.asIntBuffer().get(2 + offset)

    def solve = 0
  }

  def apply(i: Int) = Equation(bytes, EquationDataHolder.IntegersInEquation * i)

  private val bytes = ByteBuffer.allocate(EquationDataHolder.BufferSizeInBytes)
  private var currentIndex: Int = 0

  override def bytesTillFinished: Int = bytes.capacity - currentIndex
}

object EquationDataHolder {
  val IntegerSizeInBytes = 4
  val IntegersInEquation = 3
  val EquationNumber = 10
  val BufferSizeInBytes = IntegerSizeInBytes * IntegersInEquation * EquationNumber
}
