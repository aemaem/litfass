package lit.fass.litfass.server.script.groovy

import  groovy.lang.GroovyClassLoader
import lit.fass.litfass.server.script.ScriptEngine
import lit.fass.litfass.server.script.ScriptLanguage
import lit.fass.litfass.server.script.ScriptLanguage.GROOVY
import org.apache.commons.lang3.time.DurationFormatUtils.formatDurationHMS
import org.jetbrains.kotlin.utils.addToStdlib.measureTimeMillisWithResult
import org.slf4j.LoggerFactory
import java.net.URL
import java.net.URLClassLoader
import java.time.ZoneOffset.UTC
import java.time.format.DateTimeFormatter.ISO_DATE_TIME
import javax.script.ScriptEngineManager

/**
 * @author Michael Mair
 */
class GroovyScriptEngine : ScriptEngine {
    companion object {
        private val lang = GROOVY
        private val log = LoggerFactory.getLogger(this::class.java.enclosingClass)
        private val scriptLog = LoggerFactory.getLogger("$lang.Script")
        private val scriptTimestampFormatter = ISO_DATE_TIME.withZone(UTC)
    }

    private val classPath: List<String> = System.getProperty("java.class.path")
        .splitToSequence(":")
        .filter {
            !it.contains("*") && (
                    it.contains("groovy") ||
                            it.contains("slf4j-api") ||
                            it.contains("ext/")
                    )
        }
        .toList()

    init {
        log.debug("Adding classpath $classPath")
    }

    override fun isApplicable(language: ScriptLanguage): Boolean {
        return lang == language
    }

    override fun invoke(script: String, data: Map<String, Any?>): Map<String, Any?> {
        log.trace("Invoking script:\n$script\nwith data \n$data")
        val (elapsedTime, result) = measureTimeMillisWithResult {
            var result: Map<String, Any?> = emptyMap()
            GroovyClassLoader(URLClassLoader(classPath.map { URL("file:$it") }.toTypedArray())).use { classLoader ->
                result = with(ScriptEngineManager(classLoader).getEngineByName(lang.name.toLowerCase())) {
                    @Suppress("UNCHECKED_CAST")
                    eval(script, createBindings().apply {
                        put("log", scriptLog)
                        put("timestampFormatter", scriptTimestampFormatter)
                        put("data", data)
                    }) as Map<String, Any?>
                }
            }
            return@measureTimeMillisWithResult result
        }
        log.debug("Script executed in ${formatDurationHMS(elapsedTime)}")
        return result
    }
}