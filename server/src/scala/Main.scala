import java.net.InetSocketAddress
import java.nio.ByteBuffer
import java.nio.channels.{SelectionKey, Selector, ServerSocketChannel, SocketChannel}
import java.util.UUID
import java.util.concurrent.Executors

import holder.{EquationDataHolder, AbstractDataHolder}
import tasks.EquationComputationTask

// For conversions from java collections

import scala.collection.JavaConversions._

sealed trait SocketFlag

case object ServerFlag extends SocketFlag

case object ClientFlag extends SocketFlag

/**
 * User: nikita_kartashov
 * Date: 20.12.2014
 * Time: 16:01
 */
object Main extends App {
  val port = 45213
  val localhost = "localhost"
  val maxTreadNumber = 4
  val executor = Executors.newFixedThreadPool(maxTreadNumber)
  var taskToKeyMapping = Map[UUID, SelectionKey]()
  var dataReceiver = Map[SelectionKey, AbstractDataHolder]()

  def getAndRemoveDataHolder(key: SelectionKey) = {
    val result = Option(dataReceiver(key))
    if (dataReceiver.contains(key)) {
      dataReceiver -= key
    }
    result
  }

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
      val selectedKeys = selector.selectedKeys()
      selectedKeys.foreach { key =>
        key.attachment() match {
          case ServerFlag =>
            val serverSocketChannel = key.channel().asInstanceOf[ServerSocketChannel]
            val clientSocketChannel = Option(serverSocketChannel.accept())
            clientSocketChannel match {
              case Some(channel) =>
                println("New client")
                channel.configureBlocking(false)
                val clientKey = channel.register(
                  selector, SelectionKey.OP_READ,
                  SelectionKey.OP_WRITE)
                clientKey.attach(ClientFlag)
              case _ =>
            }
          case ClientFlag =>
            println("New data")
            val bufferSize = EquationDataHolder.bufferSizeInBytes
            val buffer = ByteBuffer.allocate(bufferSize)
            val clientChannel = key.channel().asInstanceOf[SocketChannel]
            if (key.isReadable) {
              val readBytes = clientChannel.read(buffer)
              if (readBytes > 0) {
                if (!dataReceiver.contains(key)) {
                  dataReceiver += (key -> new EquationDataHolder())
                }
                val holder = dataReceiver(key)
                holder.write(buffer, readBytes)
                if (holder.isFinished) {
                  // Remove holder, add task
                  val optionHolder = getAndRemoveDataHolder(key)
                  optionHolder match {
                    case Some(holder) =>
                      val newTask = EquationComputationTask(
                        holder.asInstanceOf[EquationDataHolder],
                        executor,
                        taskToKeyMapping)
                      taskToKeyMapping += (newTask.uuid -> key)
                      println("Filled data holder")
                      executor.submit(newTask)
                    case _ =>
                  }
                }
              }
              if (readBytes < 0) {
                println("Client connection closed")
                getAndRemoveDataHolder(key)
              }
            }
        }
      }
    }
  }
}