package ru.spbau.server.run

import java.nio.channels.SelectionKey
import java.util.UUID
import java.util.concurrent.ExecutorService

/**
 * User: nikita_kartashov
 * Date: 23.12.2014
 * Time: 18:00
 */
trait AbstractApplication {
  def getExecutor: ExecutorService

  def getMapping: UUID => SelectionKey

  // Means debug print
  def d(message: String): Unit
}
