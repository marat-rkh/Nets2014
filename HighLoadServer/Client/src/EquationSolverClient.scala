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
    val coeffs = List.tabulate(EQUATIONS_NUMBER * COEFFICIENTS_NUMBER)(i => coeffsGenerator.nextInt())
    val buffer = ByteBuffer.allocate(EQUATIONS_NUMBER * COEFFICIENTS_NUMBER * INTEGER_BYTES)
    val bytes = coeffs.foldLeft(buffer)((buf, coeff) => buf.putInt(coeff)).array()
    out.write(bytes)
    Utils.debug("Task has been sent")
  }
  private def readResponse() = {
    Utils.debug("Start reading response")
    val buffer = new Array[Byte](EQUATIONS_NUMBER * Integer.SIZE)
    while(in.read(buffer) > 0) {
      Utils.debug("read")
    }
    Utils.debug("Response has been received")
  }
}
