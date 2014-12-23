package ru.spbau.server.run

/**
 * User: nikita_kartashov
 * Date: 21.12.2014
 * Time: 22:26
 */
sealed trait SocketFlag

case object ServerFlag extends SocketFlag

case object ClientFlag extends SocketFlag