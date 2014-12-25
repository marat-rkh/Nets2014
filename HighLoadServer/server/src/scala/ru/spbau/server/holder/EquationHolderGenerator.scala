package ru.spbau.server.holder

import java.nio.ByteBuffer
import java.nio.channels.SelectionKey

import ru.spbau.server.run.AbstractApplication
import ru.spbau.server.tasks.EquationComputationTask

/**
 * User: nikita_kartashov
 * Date: 20.12.2014
 * Time: 17:51
 */
case class EquationHolderGenerator(app: AbstractApplication, key: SelectionKey) extends AbstractDataReceiver(app) {
  def copyToNewBuffer(srcBuffer: ByteBuffer,
                      srcOffset: Int,
                      dstBuffer: ByteBuffer,
                      dstOffset: Int = 0,
                      length: Int): Unit = {
    (0 until length).foreach { i =>
      dstBuffer.put(dstOffset + i, srcBuffer.get(srcOffset + i))
    }
  }

  override def write(newBytes: ByteBuffer, srcOffset: Int = 0, length: Int) = {
    copyToNewBuffer(newBytes, srcOffset, bytes, currentIndex, length)
    currentIndex += length
    handleReceived()
  }

  def handleReceived(): Unit = {
    var notFinished = currentIndex > sizeBytes
    var start = 0
    while (notFinished) {
      val equationsNumber = bytes.asIntBuffer().get(start / EquationHolderGenerator.IntegerSizeInBytes)
      val equationsSizeInBytes = equationsNumber * EquationHolderGenerator.EquationSizeInBytes
      // From start take equations and put into task, if possible, then move start
      if (currentIndex >= equationsSizeInBytes + sizeBytes + start) {
        val newBytes = ByteBuffer.allocate(equationsNumber * EquationHolderGenerator.EquationSizeInBytes)
        copyToNewBuffer(bytes, start + sizeBytes, newBytes, 0, equationsSizeInBytes)
        val newEquationsHolder = BufferEquationHolder(newBytes, equationsNumber)
        val newTask = EquationComputationTask(newEquationsHolder, app)
        app.getTaskToSelectionKeyMapping += (newTask.uuid -> key)
        app.getExecutor.execute(newTask)

        start += sizeBytes + equationsSizeInBytes
        notFinished = currentIndex > sizeBytes + start
      } else {
        notFinished = false
      }
    }
    if (start != 0) {
      copyToNewBuffer(bytes, start, bytes, 0, currentIndex - start)
      currentIndex -= start
    }
  }

  val sizeBytes = EquationHolderGenerator.IntegerSizeInBytes

  private val bytes = ByteBuffer.allocate(EquationHolderGenerator.MaxBufferSizeInBytes * 2)
  private var currentIndex: Int = 0
}

object EquationHolderGenerator {
  val IntegerSizeInBytes = Integer.SIZE / 8
  val IntegersInEquation = 3
  val MinEquations = 1
  val MaxEquations = 150 * 2
  val EquationSizeInBytes = IntegerSizeInBytes * IntegersInEquation
  val MaxBufferSizeInBytes = EquationSizeInBytes * MaxEquations + IntegerSizeInBytes
}
