package lit.fass.server.script

/**
 * @author Michael Mair
 */
class ScriptException(message: String, cause: Throwable) extends RuntimeException(message, cause) {

  def this(message: String) {
    this(message, null)
  }
}
