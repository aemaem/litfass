package lit.fass.litfass.server.execution

import lit.fass.litfass.server.config.yaml.model.CollectionConfig

/**
 * @author Michael Mair
 */
interface ExecutionService {

    fun execute(config: CollectionConfig, data: Collection<Map<String, Any?>>)
}