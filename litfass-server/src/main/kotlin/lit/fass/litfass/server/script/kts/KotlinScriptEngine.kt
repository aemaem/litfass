package lit.fass.litfass.server.script.kts

import lit.fass.litfass.server.script.ScriptEngine
import org.apache.commons.lang3.time.DurationFormatUtils.formatDurationHMS
import org.jetbrains.kotlin.utils.addToStdlib.measureTimeMillisWithResult
import org.slf4j.LoggerFactory
import javax.script.ScriptEngineManager

/**
 * @author Michael Mair
 */
class KotlinScriptEngine : ScriptEngine {
    companion object {
        const val EXTENSION = "kts"
        private val log = LoggerFactory.getLogger(this::class.java.enclosingClass)
        private val scriptLog = LoggerFactory.getLogger("$EXTENSION.Script")
    }

    private val scriptEngine: javax.script.ScriptEngine = ScriptEngineManager().getEngineByExtension(EXTENSION)

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
                    put("data", data)
                }) as Map<String, Any?>
            }
        }
        log.debug("Script executed in ${formatDurationHMS(elapsedTime)}")
        return result
    }
}