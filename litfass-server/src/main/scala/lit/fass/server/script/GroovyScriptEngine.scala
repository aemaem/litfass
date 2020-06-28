package lit.fass.server.script

import java.net.{URL, URLClassLoader}
import java.time.ZoneOffset.UTC
import java.time.format.DateTimeFormatter.ISO_DATE_TIME
import java.util

import groovy.lang.GroovyClassLoader
import javax.script.ScriptEngineManager
import lit.fass.server.ScriptLanguage
import lit.fass.server.ScriptLanguage.ScriptLanguage
import lit.fass.server.model.{Data, DataList}
import lit.fass.server.script.GroovyScriptEngine._
import org.apache.commons.lang3.time.StopWatch
import org.slf4j.LoggerFactory

object GroovyScriptEngine {
  private val lang = ScriptLanguage.GROOVY
  private val log = LoggerFactory.getLogger(classOf[GroovyScriptEngine])
  private val scriptLog = LoggerFactory.getLogger(s"$lang.Script")
  private val scriptTimestampFormatter = ISO_DATE_TIME.withZone(UTC)
  private val classPath: Array[String] = System.getProperty("java.class.path")
    .split(":")
    .toList
    .filter(it => !it.contains("*") && (it.contains("groovy") || it.contains("slf4j-api") || it.contains("ext/")))
    .toArray

  def apply(): GroovyScriptEngine = {
    log.debug(s"Using classpath $classPath")
    new GroovyScriptEngine()
  }
}

/**
 * @author Michael Mair
 */
class GroovyScriptEngine extends ScriptEngine {

  override def isApplicable(language: ScriptLanguage): Boolean = lang == language

  override def invoke(script: String, data: DataList): DataList = {
    log.trace(s"Invoking script:\n$script\nwith data \n$data")
    val result = DataList()

    val stopWatch = StopWatch.createStarted()

    var classLoader: GroovyClassLoader = null
    try {
      classLoader = new GroovyClassLoader(new URLClassLoader(classPath.map(it => new URL(s"file:$it"))))
      val scriptEngineManager = new ScriptEngineManager(classLoader).getEngineByName(lang.toString.toLowerCase)
      val bindings = scriptEngineManager.createBindings()
      bindings.put("log", scriptLog)
      bindings.put("timestampFormatter", scriptTimestampFormatter)
      bindings.put("data", if (data.size == 1) data.get(0) else data)
      val evalResult = scriptEngineManager.eval(script, bindings)

      evalResult match {
        case _: util.Collection[util.Map[String, Object]] =>
          evalResult.asInstanceOf[util.Collection[util.Map[String, Object]]].forEach(it => result.add(Data.of(it)))
        case _: util.Map[String, Object] =>
          result.add(Data.of(evalResult.asInstanceOf[util.Map[String, Object]]))
        case _ =>
          log.error(s"Unable to process evaluation result of type ${evalResult.getClass.getName}")
      }
    } finally {
      classLoader.close()
    }

    stopWatch.stop()
    log.debug(s"Script executed in ${stopWatch.formatTime()}")
    result
  }
}
