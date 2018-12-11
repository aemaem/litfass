package lit.fass.litfass.server.flow

import lit.fass.litfass.server.config.yaml.model.*
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
        config.flows
            .filter { isApplicable(data, it.applyIf) }
            .forEach { currentData = executeFlow(currentData, it) }
        return currentData
    }

    private fun executeFlow(data: Map<String, Any?>, flowConfig: CollectionFlowConfig): Map<String, Any?> {
        var currentData = data
        flowConfig.steps.forEach { currentData = executeStep(currentData, it) }
        return currentData
    }

    private fun executeStep(
        data: Map<String, Any?>,
        flowStepConfig: AbstractCollectionFlowStepConfig
    ): Map<String, Any?> {
        log.debug("Executing step with description ${flowStepConfig.description}")
        when (flowStepConfig) {
            is CollectionFlowStepHttpConfig -> {
                val httpResult = httpService.get(
                    replaceVariables(flowStepConfig.url, data),
                    replaceVariables(flowStepConfig.username ?: "", data),
                    replaceVariables(flowStepConfig.password ?: "", data)
                )
                return data + httpResult
            }
            is CollectionFlowStepScriptConfig -> {
                val scriptEngine = scriptEngines.find { it.isApplicable(flowStepConfig.extension) }
                    ?: throw FlowException("No script engine available for extension ${flowStepConfig.extension}")
                return scriptEngine.invoke(flowStepConfig.code, data)
            }
            else -> throw FlowException("Unknown component config ${flowStepConfig::class}")
        }
    }

    private fun isApplicable(data: Map<String, Any?>, applyIfData: Map<String, Any?>): Boolean {
        return applyIfData.isEmpty() || applyIfData.any { data[it.key] != null && data[it.key] == it.value }
    }

    private fun replaceVariables(string: String, values: Map<String, Any?>): String {
        if (string.isBlank()) {
            log.debug("Cannot replace any variables because string is empty")
            return string
        }
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