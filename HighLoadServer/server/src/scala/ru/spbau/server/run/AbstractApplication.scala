package ru.spbau.server.run

import java.nio.channels.SelectionKey
import java.util.UUID
import java.util.concurrent.ExecutorService

import ru.spbau.server.holder.AbstractDataHolder

import scala.collection.mutable

/**
 * User: nikita_kartashov
 * Date: 23.12.2014
 * Time: 18:00
 */
trait AbstractApplication {
  def getExecutor: ExecutorService

  def getTaskToSelectionKeyMapping: mutable.Map[UUID, SelectionKey]

  def getSelectionToHolderMap: mutable.Map[SelectionKey, AbstractDataHolder]

  def getAndRemoveDataHolder(key: SelectionKey): Option[AbstractDataHolder]

  // Means debug print
  def d(message: String): Unit
}
