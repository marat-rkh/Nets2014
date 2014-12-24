package ru.spbau.server.tasks

import java.nio.IntBuffer
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
    val clientKey = app.getMapping(uuid)
    val writeBuffer = IntBuffer.wrap(result)
    Option(clientKey.channel()) match {
      case Some(channel) =>
        val socketChannel = channel.asInstanceOf[SocketChannel]
        // I have commented 'while' out because after 'write' method invocation buffer's
        // position in not advanced (and we get inf loop). I assume for now that all data
        // will be sent in one call
//        while (writeBuffer.hasRemaining) {
          app.d(s"Position: ${writeBuffer.position}")
          socketChannel.write(writeBuffer)
//        }
        clientKey.cancel()
        channel.close()
      case None =>
    }
    app.d(s"Sent task $uuid")
  }
}
