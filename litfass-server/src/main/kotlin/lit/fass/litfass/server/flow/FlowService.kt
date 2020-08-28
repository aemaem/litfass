package lit.fass.litfass.server.flow

import lit.fass.litfass.server.config.yaml.model.CollectionConfig

/**
 * @author Michael Mair
 */
interface FlowService {

    fun execute(data: Collection<Map<String, Any?>>, config: CollectionConfig): CollectionFlowResult
}