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

  def exchangeInfo(): Long = {
    val startTime = System.currentTimeMillis()
    sendRandomTask()
    readResponse()
    startTime - System.currentTimeMillis()
  }

  def close(): Unit = {
    if(socket != null)
      socket.close()
  }

  private def sendRandomTask() = {
    val a = coeffsGenerator.nextInt()
    val b = coeffsGenerator.nextInt()
    val c = coeffsGenerator.nextInt()
    val coeffs = a :: b :: c :: Nil
    val buffer = ByteBuffer.allocate(12)
    val bytes = coeffs.foldLeft(buffer)((buf, coeff) => buf.putInt(coeff)).array()
    out.write(bytes)
  }
  private def readResponse() = in.read()
}
