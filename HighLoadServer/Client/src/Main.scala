/**
 * Created by mrx on 18.12.14.
 */
import java.net.InetAddress

object Main {
  def main(args: Array[String]) {
    args.length > 2 match {
      case false =>
        println("usage: <exec_name> <server_ip> <server_port> <equations_num>")
      case true =>
        try {
          runClient(args(0), Integer.parseInt(args(1)), Integer.parseInt(args(2)))
        } catch {
          case e : Exception =>
            println("error: " + e.getMessage)
        }
    }
  }

  private def runClient(ipStr: String, port: Int, equationsNum: Int) = {
    using(new EquationSolverClient(InetAddress.getByName(ipStr), port, equationsNum)) { client =>
      val res = client.exchangeInfo()
      // write to file for creating graph
      println(res)
    }
    Utils.debug("All requests have been sent")
  }

  private def using[T <: AutoCloseable, S](res: => T)(f: T => S): S = {
    try {
      f(res)
    } finally {
      if (res != null) res.close()
    }
  }
}
