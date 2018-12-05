package lit.fass.litfass.server.flow

import lit.fass.litfass.server.config.yaml.CollectionConfig
import lit.fass.litfass.server.script.ScriptEngine

/**
 * @author Michael Mair
 */
interface FlowService {

    fun execute(data: Map<String, Any?>, config: CollectionConfig, scriptEngines: List<ScriptEngine>): Map<String, Any?>
}