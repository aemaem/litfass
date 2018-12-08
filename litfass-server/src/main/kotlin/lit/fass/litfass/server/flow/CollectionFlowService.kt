package lit.fass.litfass.server.flow

import lit.fass.litfass.server.config.yaml.CollectionComponentConfig
import lit.fass.litfass.server.config.yaml.CollectionComponentHttpConfig
import lit.fass.litfass.server.config.yaml.CollectionComponentScriptConfig
import lit.fass.litfass.server.config.yaml.CollectionConfig
import lit.fass.litfass.server.http.HttpService
import lit.fass.litfass.server.script.ScriptEngine
import org.slf4j.LoggerFactory

/**
 * @author Michael Mair
 */
class CollectionFlowService(
    private val httpService: HttpService,
    private val scriptEngines: List<ScriptEngine>
) : FlowService {
    companion object {
        private val log = LoggerFactory.getLogger(this::class.java.enclosingClass)
        @Suppress("RegExpRedundantEscape")
        private val variableRegex = Regex("\\$\\{(\\w+)\\}")
    }

    override fun execute(data: Map<String, Any?>, config: CollectionConfig): Map<String, Any?> {
        var currentData = data
        config.flow.forEach { currentData = executeStep(currentData, it) }
        return currentData
    }

    private fun executeStep(data: Map<String, Any?>, componentConfig: CollectionComponentConfig): Map<String, Any?> {
        log.debug("Executing step with description ${componentConfig.description}")
        when (componentConfig) {
            is CollectionComponentHttpConfig -> {
                val httpResult = httpService.get(
                    replaceVariables(componentConfig.url, data),
                    componentConfig.username,
                    componentConfig.password
                )
                return data + httpResult
            }
            is CollectionComponentScriptConfig -> {
                val scriptEngine = scriptEngines.find { it.isApplicable(componentConfig.language) }
                    ?: throw FlowException("No script engine available for extension ${componentConfig.language}")
                return scriptEngine.invoke(componentConfig.code, data)
            }
            else -> throw FlowException("Unknown component config ${componentConfig::class}")
        }
    }

    private fun replaceVariables(string: String, values: Map<String, Any?>): String {
        if (values.isEmpty()) {
            log.debug("Cannot replace any variables because values are empty")
            return string
        }
        val variables = variableRegex.findAll(string)
        if (variables.none()) {
            log.debug("No variables found in string $string")
            return string
        }

        var replacedString = string
        variables.forEach {
            replacedString = replacedString.replace(it.value, values[it.groupValues[1]].toString())
        }
        log.debug("Replaced $string with variables ${variables.joinToString { it.groupValues[1] }} to string $replacedString")
        return replacedString
    }
}