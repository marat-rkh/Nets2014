package ru.spbau.server.run

import java.net.InetSocketAddress
import java.nio.channels.{SelectionKey, Selector, ServerSocketChannel}
import java.util.UUID
import java.util.concurrent.Executors

import ru.spbau.server.holder.AbstractDataReceiver
import ru.spbau.server.tasks.ClientReadTask

import scala.collection.mutable

/**
 * User: nikita_kartashov
 * Date: 21.12.2014
 * Time: 22:25
 */
final class ServerApplication extends AbstractApplication {
  def port = 45213

//  def localhost = "192.168.1.3"
  def localhost = "localhost"

  def maxThreadNumber = 4

  def debug = false

  private val executor = Executors.newFixedThreadPool(maxThreadNumber)
  private val taskToSelectionKeyMapping = mutable.Map[UUID, SelectionKey]()
  private val dataReceiver = mutable.Map[SelectionKey, AbstractDataReceiver]()

  override def getExecutor = executor

  override def getTaskToSelectionKeyMapping = taskToSelectionKeyMapping

  override def getSelectionToHolderMap = dataReceiver

  override def getAndRemoveDataHolder(key: SelectionKey) = {
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
    ClientReadTask(key, this).run()
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
