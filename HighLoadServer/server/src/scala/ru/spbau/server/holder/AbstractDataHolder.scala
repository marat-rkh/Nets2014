package ru.spbau.server.holder

import java.nio.ByteBuffer

/**
 * User: nikita_kartashov
 * Date: 20.12.2014
 * Time: 17:42
 */
trait AbstractDataHolder {
  def isFinished: Boolean
  def write(newBytes: ByteBuffer, length: Int)
  def bytesTillFinished: Int
}
