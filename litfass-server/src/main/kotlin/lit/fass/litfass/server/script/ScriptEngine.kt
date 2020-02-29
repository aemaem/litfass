package lit.fass.litfass.server.script

/**
 * @author Michael Mair
 */
interface ScriptEngine {
    fun isApplicable(language: ScriptLanguage): Boolean
    fun invoke(script: String, data: Collection<Map<String, Any?>>): Collection<Map<String, Any?>>
}