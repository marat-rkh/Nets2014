package ru.spbau.server.run

import java.net.InetSocketAddress
import java.nio.ByteBuffer
import java.nio.channels.{SelectionKey, Selector, ServerSocketChannel, SocketChannel}
import java.util.UUID
import java.util.concurrent.Executors

import ru.spbau.server.holder.{AbstractDataHolder, EquationDataHolder}
import ru.spbau.server.tasks.EquationComputationTask

import scala.math._

/**
 * User: nikita_kartashov
 * Date: 21.12.2014
 * Time: 22:25
 */
final class ServerApplication extends AbstractApplication {
  def port = 45213

  def localhost = "localhost"

  def maxTreadNumber = 4

  def debug = true

  private val executor = Executors.newFixedThreadPool(maxTreadNumber)
  @volatile private var taskToKeyMapping = Map[UUID, SelectionKey]()
  @volatile private var dataReceiver = Map[SelectionKey, AbstractDataHolder]()

  override def getExecutor = executor

  override def getMapping = taskToKeyMapping.apply

  def getAndRemoveDataHolder(key: SelectionKey) = {
    val result = dataReceiver.get(key)
    if (dataReceiver.contains(key)) {
      dataReceiver -= key
    }
    result
  }

  def run() = {
    val channel = ServerSocketChannel.open()
    channel.bind(new InetSocketAddress(localhost, port))
    // Set to non-blocking mode
    channel.configureBlocking(false)
    val selector = Selector.open()

    val socketServerSelectionKey = channel.register(selector,
      SelectionKey.OP_ACCEPT)
    // Mark socket as server one, to distinguish new connections
    // from old ones
    socketServerSelectionKey.attach(ServerFlag)
    while (true) {
      if (selector.select() != 0) {
        val iterator = selector.selectedKeys().iterator()
        while (iterator.hasNext) {
          val key = iterator.next()
          iterator.remove()
          key.attachment() match {
            case ServerFlag =>
              handleServerSelector(selector, key)
            case ClientFlag =>
              handleClientKey(key)
          }
        }
      }
    }
  }

  def handleClientKey(key: SelectionKey) {
    d("New data")
    val bufferSize = EquationDataHolder.BufferSizeInBytes
    val buffer = ByteBuffer.allocate(bufferSize)
    val clientChannel = key.channel().asInstanceOf[SocketChannel]
    if (key.isReadable) {
      val readBytes = clientChannel.read(buffer)
      d(s"Read $readBytes bytes")
      if (readBytes > 0) {
        if (!dataReceiver.contains(key)) {
          dataReceiver += (key -> new EquationDataHolder())
        }
        val holder = dataReceiver(key)
        holder.write(buffer, min(readBytes, holder.bytesTillFinished))
        d(s"Left ${holder.bytesTillFinished} bytes")
        if (holder.isFinished) {
          // Remove holder, add task
          val optionHolder = getAndRemoveDataHolder(key)
          optionHolder match {
            case Some(holder) =>
              val newTask = EquationComputationTask(
                holder.asInstanceOf[EquationDataHolder],
                this)
              taskToKeyMapping += (newTask.uuid -> key)
              d("Filled data holder")
              executor.execute(newTask)
            case _ =>
          }
        }
      }
      if (readBytes < 0) {
        d("Client connection closed")
        getAndRemoveDataHolder(key)
        key.cancel()
      }
    }
  }

  def handleServerSelector(selector: Selector, key: SelectionKey) {
    val serverSocketChannel = key.channel().asInstanceOf[ServerSocketChannel]
    val clientSocketChannel = Option(serverSocketChannel.accept())
    clientSocketChannel match {
      case Some(channel) =>
        d("New client")
        channel.configureBlocking(false)
        val clientKey = channel.register(
          selector, SelectionKey.OP_READ,
          SelectionKey.OP_WRITE)
        clientKey.attach(ClientFlag)
      case _ =>
    }
  }

  override def d(message: String): Unit = {
    if (debug) {
      println(message)
    }
  }
}
