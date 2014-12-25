package ru.spbau.server.tasks

import java.nio.ByteBuffer
import java.nio.channels.{SelectionKey, SocketChannel}

import ru.spbau.server.holder.EquationHolderGenerator
import ru.spbau.server.run.AbstractApplication

/**
 * User: nikita_kartashov
 * Date: 24.12.2014
 * Time: 23:19
 */
case class ClientReadTask(key: SelectionKey, app: AbstractApplication) extends Runnable {
  override def run(): Unit = {
    app.d("New data")
    val bufferSize = EquationHolderGenerator.MaxBufferSizeInBytes
    val buffer = ByteBuffer.allocate(bufferSize)
    val clientChannel = key.channel().asInstanceOf[SocketChannel]
    if (key.isValid && key.isReadable) {
      val readBytes = clientChannel.read(buffer)
      app.d(s"Read $readBytes bytes")
      if (readBytes > 0) {
        if (!app.getSelectionToHolderMap.contains(key)) {
          app.getSelectionToHolderMap += (key -> new EquationHolderGenerator(app, key))
        }
        val holder = app.getSelectionToHolderMap(key)
        holder.write(buffer, 0, readBytes)
      } else if (readBytes < 0) {
        app.d("Client connection closed")
        app.getAndRemoveDataHolder(key)
        clientChannel.close()
      }
    }
  }
}
