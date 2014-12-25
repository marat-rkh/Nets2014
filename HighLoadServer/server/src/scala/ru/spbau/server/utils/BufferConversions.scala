package ru.spbau.server.utils

import java.nio.{ByteBuffer, IntBuffer}

import ru.spbau.server.holder.EquationHolderGenerator

/**
 * User: nikita_kartashov
 * Date: 23.12.2014
 * Time: 19:08
 */
object BufferConversions {
  implicit def asByteBuffer(buffer: IntBuffer): ByteBuffer = {
    val byteBuffer = ByteBuffer.allocate(buffer.limit * EquationHolderGenerator.IntegerSizeInBytes)
    byteBuffer.asIntBuffer().put(buffer.array())
    byteBuffer
  }
}
