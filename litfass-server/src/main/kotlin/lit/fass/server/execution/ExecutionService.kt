package lit.fass.server.execution

import lit.fass.server.config.yaml.model.CollectionConfig

/**
 * @author Michael Mair
 */
interface ExecutionService {

    fun execute(config: CollectionConfig, data: Collection<Map<String, Any?>>)
}