import java.io._

/**
 * Created by mrx on 25.12.14.
 */

class Logger(filePath: String) extends AutoCloseable {
  val logWriter = new BufferedWriter(new FileWriter(filePath))
  val DEBUG = true
  val LOG = true

  var firstMsg = true

  logWriter.write("{\n")

  def log(title: String, msg: String) = {
    val fullMsg = title + " : " + msg
    if(LOG) {
      if(!firstMsg)
        logWriter.write(",\n")
      logWriter.write(fullMsg)
      firstMsg = false
    }
    if(DEBUG) {
      println(fullMsg)
    }
  }

  def close() = {
    logWriter.write("\n}")
    logWriter.close()
  }
}
