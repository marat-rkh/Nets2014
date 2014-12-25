/**
 * Created by mrx on 18.12.14.
 */
import java.net._
import java.nio.ByteBuffer
import scala.util.Random

class EquationSolverClient(ip: InetAddress, port: Int) extends AutoCloseable {
  val socket = new Socket(ip, port)
  val out = socket.getOutputStream
  val in = socket.getInputStream
  val coeffsGenerator = new Random()

  val EQUATIONS_NUMBER = 10
  val COEFFICIENTS_NUMBER = 3
  val INTEGER_BYTES = Integer.SIZE / 8

  def exchangeInfo(): Long = {
    val startTime = System.currentTimeMillis()
    sendRandomTask()
    readResponse()
    System.currentTimeMillis() - startTime
  }

  def close(): Unit = {
    if(socket != null)
      socket.close()
  }

  private def sendRandomTask() = {
    val MaxEquations = 150 * 2
    val equationsNumber = scala.math.abs(coeffsGenerator.nextInt() % MaxEquations + 1)
    val coeffs = List.tabulate(equationsNumber * COEFFICIENTS_NUMBER)(i => coeffsGenerator.nextInt())

    println(equationsNumber)
    val buffer = ByteBuffer.allocate(equationsNumber * COEFFICIENTS_NUMBER * INTEGER_BYTES)
    val bytes = coeffs.foldLeft(buffer)((buf, coeff) => buf.putInt(coeff)).array()
    // constant 10 for now
    val sizeBytes = ByteBuffer.allocate(4).putInt(equationsNumber).array()
    out.write(sizeBytes)
    out.write(bytes)
    Utils.debug("Task has been sent")
  }
  private def readResponse() = {
    Utils.debug("Start reading response")
    val buffer = new Array[Byte](EQUATIONS_NUMBER * INTEGER_BYTES)
    var hasNotReceivedEverything = true
    var bytesReadSoFar = 0
    while (hasNotReceivedEverything) {
      val read = in.read(buffer)
      bytesReadSoFar += read
      println(s"bytes_read: $bytesReadSoFar")
      println(s"read: $read")
      hasNotReceivedEverything = bytesReadSoFar < EQUATIONS_NUMBER * INTEGER_BYTES
    }
    Utils.debug("Response has been received")
  }
}
