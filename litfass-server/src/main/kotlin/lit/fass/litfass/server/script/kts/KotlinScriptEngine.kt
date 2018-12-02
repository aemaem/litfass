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
    }

    private val scriptEngine: javax.script.ScriptEngine = ScriptEngineManager().getEngineByExtension("kts")

    override fun invoke(script: String, input: Map<String, Any>): Any? {
        log.debug("Invoking script:\n$script\nwith input \n$input")
        with(scriptEngine) { return eval(script, createBindings().apply { putAll(input) }) }
    }
}