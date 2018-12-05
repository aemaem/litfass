package lit.fass.litfass.server.script

/**
 * @author Michael Mair
 */
interface ScriptEngine {
    fun isApplicable(extension: String): Boolean
    fun invoke(script: String, input: Map<String, Any?>): Map<String, Any?>
}