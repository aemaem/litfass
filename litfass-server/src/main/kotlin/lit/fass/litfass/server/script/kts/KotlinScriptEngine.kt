package lit.fass.litfass.server.script.kts

import lit.fass.litfass.server.script.ScriptEngine
import org.slf4j.LoggerFactory
import javax.script.ScriptEngineManager

/**
 * @author Michael Mair
 */
class KotlinScriptEngine : ScriptEngine {
    companion object {
        private val log = LoggerFactory.getLogger(this::class.java.enclosingClass)
        const val EXTENSION = "kts"
    }

    private val scriptEngine: javax.script.ScriptEngine = ScriptEngineManager().getEngineByExtension(EXTENSION)

    override fun isApplicable(extension: String): Boolean {
        return EXTENSION == extension
    }

    override fun invoke(script: String, input: Map<String, Any?>): Map<String, Any?> {
        log.debug("Invoking transform:\n$script\nwith input \n$input")
        with(scriptEngine) { return eval(script, createBindings().apply { putAll(input) }) as Map<String, Any?> }
    }
}