package lit.fass.litfass.server.flow

import lit.fass.litfass.server.config.yaml.model.*
import lit.fass.litfass.server.http.HttpService
import lit.fass.litfass.server.script.ScriptEngine
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

/**
 * @author Michael Mair
 */
@Service
class CollectionFlowService(
    private val httpService: HttpService,
    private val scriptEngines: List<ScriptEngine>
) : FlowService {
    companion object {
        private val log = LoggerFactory.getLogger(this::class.java.enclosingClass)
        @Suppress("RegExpRedundantEscape")
        private val variableRegex = Regex("\\$\\{(\\w+)\\}")
    }

    override fun execute(data: Collection<Map<String, Any?>>, config: CollectionConfig): Collection<Map<String, Any?>> {
        var currentData = data
        config.flows
            .filter { isApplicable(data.first(), it.applyIf) }
            .forEach { currentData = executeFlow(currentData, it) }
        return currentData
    }

    private fun executeFlow(data: Collection<Map<String, Any?>>, flowConfig: CollectionFlowConfig): Collection<Map<String, Any?>> {
        var currentData = data
        flowConfig.steps.forEach { currentData = executeStep(currentData, it) }
        return currentData
    }

    private fun executeStep(
        data: Collection<Map<String, Any?>>,
        flowStepConfig: AbstractCollectionFlowStepConfig
    ): Collection<Map<String, Any?>> {
        log.debug("Executing step with description: ${flowStepConfig.description}")
        when (flowStepConfig) {
            is CollectionFlowStepHttpConfig -> {
                val httpResult = httpService.get(
                    replaceVariables(flowStepConfig.url, data),
                    flowStepConfig.headers?.map {
                        it["name"]!! to replaceVariables(it["value"] ?: "", data)
                    }?.toMap() ?: emptyMap(),
                    replaceVariables(flowStepConfig.username ?: "", data),
                    replaceVariables(flowStepConfig.password ?: "", data)
                )
                return if (data.size == 1) listOf(data.first() + httpResult) else data + httpResult
            }
            is CollectionFlowStepScriptConfig -> {
                val scriptEngine = scriptEngines.find { it.isApplicable(flowStepConfig.language) }
                    ?: throw FlowException("No script engine available for language ${flowStepConfig.language}")
                return scriptEngine.invoke(flowStepConfig.code, data)
            }
            else -> throw FlowException("Unknown component config ${flowStepConfig::class}")
        }
    }

    internal fun isApplicable(data: Map<String, Any?>, applyIfData: Map<String, Any?>): Boolean {
        return applyIfData.isEmpty() || applyIfData.any { data[it.key] != null && data[it.key] == it.value }
    }

    internal fun replaceVariables(string: String, values: Collection<Map<String, Any?>>): String {
        if (string.isBlank()) {
            log.debug("Cannot replace any variables because string is empty")
            return string
        }
        if (values.isEmpty() || values.first().isEmpty()) {
            log.debug("Cannot replace any variables because values are empty")
            return string
        }
        val variables = variableRegex.findAll(string)
        if (variables.none()) {
            log.trace("No variables found in string $string")
            return string
        }

        var replacedString = string
        variables.forEach { variable ->
            values.forEach { dataEntry ->
                replacedString = replacedString.replace(variable.value, dataEntry[variable.groupValues[1]].toString())
            }
        }
        log.trace("Replaced $string with variables ${variables.joinToString { it.groupValues[1] }} to string $replacedString")
        return replacedString
    }
}