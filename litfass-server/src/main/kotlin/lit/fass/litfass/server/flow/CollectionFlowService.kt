package lit.fass.litfass.server.flow

import lit.fass.litfass.server.config.yaml.CollectionComponentConfig
import lit.fass.litfass.server.config.yaml.CollectionComponentRequestConfig
import lit.fass.litfass.server.config.yaml.CollectionComponentTransformConfig
import lit.fass.litfass.server.config.yaml.CollectionConfig
import lit.fass.litfass.server.script.ScriptEngine
import org.slf4j.LoggerFactory

/**
 * @author Michael Mair
 */
class CollectionFlowService : FlowService {
    companion object {
        private val log = LoggerFactory.getLogger(this::class.java.enclosingClass)
    }

    override fun execute(
        data: Map<String, Any?>,
        config: CollectionConfig,
        scriptEngines: List<ScriptEngine>
    ): Map<String, Any?> {
        var currentData = data
        config.flow.forEach {
            currentData = executeStep(currentData, it, scriptEngines)
        }
        return currentData
    }

    private fun executeStep(
        data: Map<String, Any?>,
        componentConfig: CollectionComponentConfig,
        scriptEngines: List<ScriptEngine>
    ): Map<String, Any?> {
        log.debug("Executing step with description ${componentConfig.description}")
        return when (componentConfig) {
            is CollectionComponentRequestConfig -> throw UnsupportedOperationException("Not yet implemented")
            is CollectionComponentTransformConfig -> {
                val scriptEngine = scriptEngines.find { it.isApplicable(componentConfig.language) }
                    ?: throw FlowException("No script engine available for extension ${componentConfig.language}")
                scriptEngine.invoke(componentConfig.code, data)
            }
            else -> throw FlowException("Unknown component config ${componentConfig::class}")
        }
    }
}