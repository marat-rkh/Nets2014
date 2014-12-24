package ru.spbau.server.tasks

import java.nio.channels.SelectionKey

/**
 * User: nikita_kartashov
 * Date: 24.12.2014
 * Time: 23:19
 */
case class ClientReadTask(key: SelectionKey) extends Runnable{
  override def run(): Unit = println()
}
