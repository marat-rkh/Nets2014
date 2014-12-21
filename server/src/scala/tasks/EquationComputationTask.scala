package tasks

import java.nio.channels.SelectionKey
import java.util.UUID
import java.util.concurrent.ExecutorService

import holder.EquationDataHolder

/**
 * User: nikita_kartashov
 * Date: 20.12.2014
 * Time: 19:33
 */
case class EquationComputationTask(equationDataHolder: EquationDataHolder,
                                   executor: ExecutorService,
                                   taskToKeyMapping: Map[UUID, SelectionKey]) extends Runnable {
  val uuid = UUID.randomUUID()

  override def run(): Unit = {
    val resultArray = new Array[Int](EquationDataHolder.equationNumber)
    (0 until EquationDataHolder.equationNumber).foreach{
      i =>
        val eq = equationDataHolder(i)
        val d = eq.b * eq.b - 4 * eq.a * eq.c
        if (d > 0) {
          resultArray(i) = 2
        }
        if (d == 0) {
          resultArray(i) = 1
        }
        if (d < 0) {
          resultArray(i) = 0
        }
    }
    val newTask = ResponseSendingTask(uuid, resultArray, taskToKeyMapping)
    executor.submit(newTask)
  }
}
