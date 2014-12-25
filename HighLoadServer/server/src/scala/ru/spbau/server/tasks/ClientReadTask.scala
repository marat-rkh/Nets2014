package ru.spbau.server.tasks

import java.nio.ByteBuffer
import java.nio.channels.{SocketChannel, SelectionKey}

import ru.spbau.server.holder.EquationDataHolder
import ru.spbau.server.run.AbstractApplication

import scala.math._

/**
 * User: nikita_kartashov
 * Date: 24.12.2014
 * Time: 23:19
 */
case class ClientReadTask(key: SelectionKey, app: AbstractApplication) extends Runnable {
  override def run(): Unit = {
    app.d("New data")
    val bufferSize = EquationDataHolder.BufferSizeInBytes
    val buffer = ByteBuffer.allocate(bufferSize)
    val clientChannel = key.channel().asInstanceOf[SocketChannel]
    if (key.isValid && key.isReadable) {
      val readBytes = clientChannel.read(buffer)
      app.d(s"Read $readBytes bytes")
      if (readBytes > 0) {
        if (!app.getSelectionToHolderMap.contains(key)) {
          app.getSelectionToHolderMap += (key -> new EquationDataHolder())
        }
        val holder = app.getSelectionToHolderMap(key)
        holder.write(buffer, min(readBytes, holder.bytesTillFinished))
        app.d(s"Left ${holder.bytesTillFinished} bytes")
        if (holder.isFinished) {
          // Remove holder, add task
          val optionHolder = app.getAndRemoveDataHolder(key)
          optionHolder match {
            case Some(holder) =>
              val newTask = EquationComputationTask(
                holder.asInstanceOf[EquationDataHolder],
                app)
              app.getTaskToSelectionKeyMapping += (newTask.uuid -> key)
              app.d("Filled data holder")
              app.getExecutor.execute(newTask)
            case _ =>
          }
        }
      }
      if (readBytes < 0) {
        app.d("Client connection closed")
        app.getAndRemoveDataHolder(key)
        clientChannel.close()
      }
    }

  }
}
