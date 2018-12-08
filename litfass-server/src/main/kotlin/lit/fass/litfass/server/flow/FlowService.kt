package lit.fass.litfass.server.flow

import lit.fass.litfass.server.config.yaml.CollectionConfig

/**
 * @author Michael Mair
 */
interface FlowService {

    fun execute(data: Map<String, Any?>, config: CollectionConfig): Map<String, Any?>
}