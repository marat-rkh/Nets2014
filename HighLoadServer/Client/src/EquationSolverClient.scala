/**
 * Created by mrx on 18.12.14.
 */
import java.net._
import java.nio.ByteBuffer
import scala.util.Random

class EquationSolverClient(ip: InetAddress, port: Int, val EQUATIONS_NUM: Int) extends AutoCloseable {
  val socket = new Socket(ip, port)
  val out = socket.getOutputStream
  val in = socket.getInputStream
  val coeffsGenerator = new Random()

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
    val coeffs = List.tabulate(EQUATIONS_NUM * COEFFICIENTS_NUMBER)(i => coeffsGenerator.nextInt())
    val buffer = ByteBuffer.allocate(EQUATIONS_NUM * COEFFICIENTS_NUMBER * INTEGER_BYTES)
    val data = coeffs.foldLeft(buffer)((buf, coeff) => buf.putInt(coeff)).array()
    val sizeBytes = ByteBuffer.allocate(4).putInt(EQUATIONS_NUM).array()
    out.write(sizeBytes)
    out.write(data)
    Utils.debug("Task has been sent")
  }
  private def readResponse() = {
    Utils.debug("Start reading response")
    val buffer = new Array[Byte](EQUATIONS_NUM * INTEGER_BYTES)
    var hasNotReceivedEverything = true
    var bytesReadSoFar = 0
    while (hasNotReceivedEverything) {
      val read = in.read(buffer)
      bytesReadSoFar += read
      println(s"bytes_read: $bytesReadSoFar")
      println(s"read: $read")
      hasNotReceivedEverything = bytesReadSoFar < EQUATIONS_NUM * INTEGER_BYTES
    }
    Utils.debug("Response has been received")
  }
}
