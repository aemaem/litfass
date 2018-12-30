package lit.fass.litfass.server.script

/**
 * @author Michael Mair
 */
interface ScriptEngine {
    fun isApplicable(language: ScriptLanguage): Boolean
    fun invoke(script: String, data: Map<String, Any?>): Map<String, Any?>
}