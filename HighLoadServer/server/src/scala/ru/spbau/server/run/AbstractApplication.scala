package ru.spbau.server.run

import java.nio.channels.SelectionKey
import java.util.UUID
import java.util.concurrent.ExecutorService

import ru.spbau.server.holder.AbstractDataReceiver

import scala.collection.mutable

/**
 * User: nikita_kartashov
 * Date: 23.12.2014
 * Time: 18:00
 */
trait AbstractApplication {
  def getExecutor: ExecutorService

  def getTaskToSelectionKeyMapping: mutable.Map[UUID, SelectionKey]

  def getSelectionToHolderMap: mutable.Map[SelectionKey, AbstractDataReceiver]

  def getAndRemoveDataHolder(key: SelectionKey): Option[AbstractDataReceiver]

  // Means debug print
  def d(message: String): Unit
}
