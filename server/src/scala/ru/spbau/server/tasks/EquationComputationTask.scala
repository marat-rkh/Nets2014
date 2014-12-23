package ru.spbau.server.tasks

import java.util.UUID

import ru.spbau.server.holder.EquationDataHolder
import ru.spbau.server.run.ServerApplication

/**
 * User: nikita_kartashov
 * Date: 20.12.2014
 * Time: 19:33
 */
case class EquationComputationTask(equationDataHolder: EquationDataHolder,
                                   app: ServerApplication) extends Runnable {
  val uuid = UUID.randomUUID()

  override def run(): Unit = {
    println(s"Started evaluating task $uuid")
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
    val newTask = ResponseSendingTask(uuid, resultArray, app)
    println(s"Finished evaluating task $uuid")
    app.getExecutor.execute(newTask)
  }
}
