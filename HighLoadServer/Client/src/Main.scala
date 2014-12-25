/**
 * Created by mrx on 18.12.14.
 */
import java.net.InetAddress

object Main {
  def main(args: Array[String]) {
    args.length > 3 match {
      case false =>
        println("usage: <exec_name> <server_ip> <server_port> <equations_num> <log_file_name>")
      case true =>
        try {
          runClient(args(0), Integer.parseInt(args(1)), Integer.parseInt(args(2)), args(3))
        } catch {
          case e : Exception =>
            println("error: " + e.getMessage)
        }
    }
  }

  private def runClient(ipStr: String, port: Int, equationsNum: Int, logFilePath: String) = {
    val logger = new Logger(logFilePath)
    using(new EquationSolverClient(InetAddress.getByName(ipStr),
                                   port,
                                   equationsNum,
                                   logger))
    {
      client => client.exchangeInfo()
    }
  }

  private def using[T <: AutoCloseable, S](res: => T)(f: T => S): S = {
    try {
      f(res)
    } finally {
      if (res != null) res.close()
    }
  }
}
