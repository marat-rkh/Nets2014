package ru.spbau.server.tasks

import java.nio.{ByteBuffer, IntBuffer}
import java.nio.channels.SocketChannel
import java.util.UUID

import ru.spbau.server.run.AbstractApplication
import ru.spbau.server.utils.BufferConversions._

/**
 * User: nikita_kartashov
 * Date: 20.12.2014
 * Time: 20:13
 */
case class ResponseSendingTask(uuid: UUID, result: Array[Int], app: AbstractApplication) extends Runnable {
  override def run(): Unit = {
    app.d(s"Started sending task $uuid")
    val clientKey = app.getTaskToSelectionKeyMapping(uuid)
    val writeBuffer:ByteBuffer = IntBuffer.wrap(result)
    Option(clientKey.channel()) match {
      case Some(channel) =>
        val socketChannel = channel.asInstanceOf[SocketChannel]
        while (writeBuffer.hasRemaining) {
          val written = socketChannel.write(writeBuffer)
          app.d(s"Written: $written")
          app.d(s"Position: ${writeBuffer.position}")
        }
      case None =>
    }
    app.getTaskToSelectionKeyMapping -= uuid
    app.d(s"Sent task $uuid")
  }
}
