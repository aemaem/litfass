package lit.fass.server.flow

import lit.fass.server.config.yaml.model.CollectionConfig

/**
 * @author Michael Mair
 */
interface FlowService {

    fun execute(data: Collection<Map<String, Any?>>, config: CollectionConfig): FlowResponse
}