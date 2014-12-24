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
  val taskSize = 30

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
    val coeffs = List.tabulate(taskSize)(i => coeffsGenerator.nextInt())
    val buffer = ByteBuffer.allocate(taskSize * 4)
    val bytes = coeffs.foldLeft(buffer)((buf, coeff) => buf.putInt(coeff)).array()
    out.write(bytes)
  }
  private def readResponse() = {
    var buffer = new Array[Byte](1024)
    while(in.read(buffer) != -1) {}
  }
}
