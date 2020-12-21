package lit.fass.server.execution

import lit.fass.server.config.yaml.model.CollectionConfig
import lit.fass.server.flow.FlowAction.ADD
import lit.fass.server.flow.FlowAction.REMOVE
import lit.fass.server.flow.FlowService
import lit.fass.server.persistence.CollectionPersistenceService
import lit.fass.server.persistence.CollectionPersistenceService.Companion.ID_KEY

/**
 * @author Michael Mair
 */
class CollectionExecutionService(
    private val flowService: FlowService,
    private val collectionPersistenceServices: List<CollectionPersistenceService>
) : ExecutionService {

    override fun execute(config: CollectionConfig, data: Collection<Map<String, Any?>>) {
        val flowData = flowService.execute(data, config)
        val persistenceService = collectionPersistenceServices.find { it.isApplicable(config.datastore) }
            ?: throw ExecutionException("No persistence service applicable for ${config.datastore}")

        when (flowData.action) {
            ADD -> persistenceService.saveCollection(config.collection, flowData.data)
            REMOVE -> {
                if (!flowData.data.all { it.containsKey(ID_KEY) }) throw ExecutionException("Identifier field $ID_KEY must be given")
                persistenceService.removeCollection(config.collection, flowData.data.map { it[ID_KEY] as String })
            }
        }
    }
}