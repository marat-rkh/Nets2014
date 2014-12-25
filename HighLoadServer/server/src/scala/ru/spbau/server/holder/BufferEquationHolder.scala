package ru.spbau.server.holder

import java.nio.ByteBuffer

/**
 * User: nikita_kartashov
 * Date: 25.12.2014
 * Time: 17:15
 */
case class BufferEquationHolder(buffer: ByteBuffer, equationsNumber: Int) extends AbstractEquationHolder{
  override def apply(i: Int): Equation = Equation(buffer, i * EquationHolderGenerator.IntegersInEquation)

  override def getEquationsNumber: Int = equationsNumber
}
