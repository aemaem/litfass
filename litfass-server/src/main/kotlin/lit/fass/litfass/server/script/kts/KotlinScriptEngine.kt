package lit.fass.litfass.server.script.kts

import lit.fass.litfass.server.script.ScriptEngine
import org.apache.commons.lang3.time.DurationFormatUtils.formatDurationWords
import org.jetbrains.kotlin.utils.addToStdlib.measureTimeMillisWithResult
import org.slf4j.LoggerFactory
import javax.script.ScriptContext.ENGINE_SCOPE
import javax.script.ScriptEngineManager

/**
 * @author Michael Mair
 */
class KotlinScriptEngine : ScriptEngine {
    companion object {
        private val log = LoggerFactory.getLogger(this::class.java.enclosingClass)
        const val EXTENSION = "kts"
    }

    private val scriptEngine: javax.script.ScriptEngine = ScriptEngineManager().getEngineByExtension(EXTENSION).apply {
        put("log", log)
    }

    override fun isApplicable(extension: String): Boolean {
        return EXTENSION == extension
    }

    override fun invoke(script: String, data: Map<String, Any?>): Map<String, Any?> {
        log.debug("Invoking script:\n$script\nwith data \n$data")
        try {
            val (elapsedTime, result) = measureTimeMillisWithResult {
                with(scriptEngine) {
                    @Suppress("UNCHECKED_CAST")
                    eval(script, createBindings().apply { put("data", data) }) as Map<String, Any?>
                }
            }
            log.debug("Script executed in ${formatDurationWords(elapsedTime, true, true)}")
            return result
        } finally {
            scriptEngine.getBindings(ENGINE_SCOPE).remove("data")
        }
    }
}