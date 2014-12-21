package tasks

import java.nio.channels.{SelectionKey, SocketChannel}
import java.nio.{ByteBuffer, IntBuffer}
import java.util.UUID

/**
 * User: nikita_kartashov
 * Date: 20.12.2014
 * Time: 20:13
 */
case class ResponseSendingTask(uuid: UUID, result: Array[Int], taskToKeyMapping: Map[UUID, SelectionKey]) extends Runnable{
  override def run(): Unit = {
    val clientKey = taskToKeyMapping(uuid)
    val writeBuffer = IntBuffer.wrap(result)
    Option(clientKey.channel()) match {
      case Some(channel) => channel.asInstanceOf[SocketChannel].write(writeBuffer.asInstanceOf[ByteBuffer])
      case None => None
    }
  }
}
