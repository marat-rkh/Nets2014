package holder

import java.nio.ByteBuffer

/**
 * User: nikita_kartashov
 * Date: 20.12.2014
 * Time: 17:51
 */
class EquationDataHolder extends AbstractDataHolder {
  override def isFinished: Boolean = currentIndex == EquationDataHolder.bufferSizeInBytes

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

  def apply(i: Int) = Equation(bytes, EquationDataHolder.integersInEquation * i)

  private val bytes = ByteBuffer.allocate(EquationDataHolder.bufferSizeInBytes)
  private var currentIndex: Int = 0
}

object EquationDataHolder {
  val integerSizeInBytes = 4
  val integersInEquation = 3
  val equationNumber = 10
  val bufferSizeInBytes = integerSizeInBytes * integersInEquation * equationNumber
}
