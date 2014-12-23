package ru.spbau.server.tasks

import java.nio.channels.SocketChannel
import java.nio.{ByteBuffer, IntBuffer}
import java.util.UUID

import ru.spbau.server.holder.EquationDataHolder
import ru.spbau.server.run.ServerApplication

/**
 * User: nikita_kartashov
 * Date: 20.12.2014
 * Time: 20:13
 */
case class ResponseSendingTask(uuid: UUID, result: Array[Int], app: ServerApplication) extends Runnable {
  private def asByteBuffer(buffer: IntBuffer) = {
    val byteBuffer = ByteBuffer.allocate(buffer.limit * EquationDataHolder.integerSizeInBytes)
    byteBuffer.asIntBuffer().put(buffer.array())
    byteBuffer
  }

  override def run(): Unit = {
    println(s"Started sending task $uuid")
    val clientKey = app.getMapping(uuid)
    val writeBuffer = asByteBuffer(IntBuffer.wrap(result))
    Option(clientKey.channel()) match {
      case Some(channel) =>
        while (writeBuffer.hasRemaining) {
          println(s"Position: ${writeBuffer.position}")
          channel.asInstanceOf[SocketChannel].write(writeBuffer)
        }
        clientKey.cancel()
        channel.close()
      case None =>
    }
    println(s"Sent task $uuid")
  }
}
