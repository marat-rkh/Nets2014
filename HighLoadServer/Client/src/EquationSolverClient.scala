/**
 * Created by mrx on 18.12.14.
 */
import java.net._
import java.nio.ByteBuffer
import scala.util.Random

class EquationSolverClient(ip: InetAddress,
                           port: Int,
                           val EQUATIONS_NUM: Int,
                           val logger: Logger) extends AutoCloseable {
  val socket = new Socket(ip, port)
  val out = socket.getOutputStream
  val in = socket.getInputStream
  val coeffsGenerator = new Random()

  val COEFFICIENTS_NUMBER = 3
  val INTEGER_BYTES = Integer.SIZE / 8

  val K_GEN_TASK = "gen_task"
  val K_BEG_SEND = "beg_send"
  val K_END_SEND = "end_send"
  val K_BEG_RESP = "beg_resp"
  val K_END_RESP = "end_resp"

  def exchangeInfo(): Unit = {
    val startTime = System.currentTimeMillis()
    sendRandomTask(startTime)
    readResponse(startTime)
  }

  def close(): Unit = {
    if(socket != null)
      socket.close()
    logger.close()
  }

  private def sendRandomTask(startTime: Long) = {
    logger.log(K_GEN_TASK, time(startTime))
    val coeffs = List.tabulate(EQUATIONS_NUM * COEFFICIENTS_NUMBER)(i => coeffsGenerator.nextInt())
    val buffer = ByteBuffer.allocate(EQUATIONS_NUM * COEFFICIENTS_NUMBER * INTEGER_BYTES)
    val data = coeffs.foldLeft(buffer)((buf, coeff) => buf.putInt(coeff)).array()
    val sizeBytes = ByteBuffer.allocate(4).putInt(EQUATIONS_NUM).array()
    logger.log(K_BEG_SEND, time(startTime))
    out.write(sizeBytes)
    out.write(data)
    logger.log(K_END_SEND, time(startTime))
  }

  private def readResponse(startTime: Long) = {
    val buffer = new Array[Byte](EQUATIONS_NUM * INTEGER_BYTES)
    var hasNotReceivedEverything = true
    var bytesReadSoFar = 0
    logger.log(K_BEG_RESP, time(startTime))
    while (hasNotReceivedEverything) {
      val read = in.read(buffer)
      bytesReadSoFar += read
//      println(s"bytes_read: $bytesReadSoFar")
//      println(s"read: $read")
      hasNotReceivedEverything = bytesReadSoFar < EQUATIONS_NUM * INTEGER_BYTES
    }
    logger.log(K_END_RESP, time(startTime))
  }

  private def time(zeroTime: Long) = (System.currentTimeMillis() - zeroTime).toString
}
