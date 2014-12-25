package ru.spbau.server.tasks

import java.util.UUID

import ru.spbau.server.holder.AbstractEquationHolder
import ru.spbau.server.run.AbstractApplication

/**
 * User: nikita_kartashov
 * Date: 20.12.2014
 * Time: 19:33
 */
case class EquationComputationTask(equationHolder: AbstractEquationHolder,
                                   app: AbstractApplication) extends Runnable {
  val uuid = UUID.randomUUID()
  val taskCreated = System.currentTimeMillis

  override def run(): Unit = {
    app.d(s"Started evaluating task $uuid")
    val resultArray = new Array[Int](equationHolder.getEquationsNumber)
    (0 until equationHolder.getEquationsNumber).foreach {
      i =>
        val eq = equationHolder(i)
        resultArray(i) = eq.solve
    }
    val newTask = ResponseSendingTask(uuid, resultArray, app)
    app.d(s"Finished evaluating task $uuid")
    println(System.currentTimeMillis() - taskCreated)
    app.getExecutor.execute(newTask)
  }
}
