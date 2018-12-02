package lit.fass.litfass.server.script

/**
 * @author Michael Mair
 */
interface ScriptEngine {

    fun invoke(script: String, input: Map<String, Any>): Any?
}