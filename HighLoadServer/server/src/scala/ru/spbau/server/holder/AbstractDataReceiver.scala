package ru.spbau.server.holder

import java.nio.ByteBuffer

import ru.spbau.server.run.AbstractApplication

/**
 * User: nikita_kartashov
 * Date: 20.12.2014
 * Time: 17:42
 */
abstract class AbstractDataReceiver(app: AbstractApplication) {
  def write(newBytes: ByteBuffer, srcOffset: Int = 0, length: Int)
}
