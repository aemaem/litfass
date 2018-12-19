package lit.fass.litfass.server.script.kts

import lit.fass.litfass.server.script.ScriptEngine
import org.apache.commons.lang3.time.DurationFormatUtils.formatDurationHMS
import org.jetbrains.kotlin.utils.addToStdlib.measureTimeMillisWithResult
import org.slf4j.LoggerFactory
import java.time.ZoneOffset.UTC
import java.time.format.DateTimeFormatter.ISO_DATE_TIME
import javax.script.ScriptEngineManager

/**
 * @author Michael Mair
 */
class KotlinScriptEngine : ScriptEngine {
    companion object {
        const val EXTENSION = "kts"
        private val log = LoggerFactory.getLogger(this::class.java.enclosingClass)
        private val scriptLog = LoggerFactory.getLogger("$EXTENSION.Script")
        private val scriptTimestampFormatter = ISO_DATE_TIME.withZone(UTC)
    }

    private val scriptEngine: javax.script.ScriptEngine

    init {
        val classPath = System.getProperty("java.class.path")
            .splitToSequence(":")
            .filter {
                !it.contains("*") && (
                        it.contains("kotlin-script-util") ||
                                it.contains("kotlin-script-runtime") ||
                                it.contains("kotlin-stdlib") ||
                                it.contains("slf4j-api") ||
                                it.contains("ext/")
                        )
            }
            .joinToString(":")
        log.debug("Adding classpath $classPath")
        System.setProperty("kotlin.script.classpath", classPath)
        scriptEngine = ScriptEngineManager().getEngineByExtension(EXTENSION)
    }

    override fun isApplicable(extension: String): Boolean {
        return EXTENSION == extension
    }

    override fun invoke(script: String, data: Map<String, Any?>): Map<String, Any?> {
        log.debug("Invoking script:\n$script\nwith data \n$data")
        val (elapsedTime, result) = measureTimeMillisWithResult {
            with(scriptEngine) {
                @Suppress("UNCHECKED_CAST")
                eval(script, createBindings().apply {
                    put("log", scriptLog)
                    put("timestampFormatter", scriptTimestampFormatter)
                    put("data", data)
                }) as Map<String, Any?>
            }
        }
        log.debug("Script executed in ${formatDurationHMS(elapsedTime)}")
        return result
    }
}