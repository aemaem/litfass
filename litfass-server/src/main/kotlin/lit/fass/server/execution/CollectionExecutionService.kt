package lit.fass.server.execution

import lit.fass.server.config.yaml.model.CollectionConfig
import lit.fass.server.flow.FlowService
import lit.fass.server.persistence.CollectionPersistenceService

/**
 * @author Michael Mair
 */
class CollectionExecutionService(
    private val flowService: FlowService,
    private val collectionPersistenceServices: List<CollectionPersistenceService>
) : ExecutionService {

    override fun execute(config: CollectionConfig, data: Collection<Map<String, Any?>>) {
        val dataToPersist = flowService.execute(data, config)
        val persistenceService = collectionPersistenceServices.find { it.isApplicable(config.datastore) }
            ?: throw ExecutionException("No persistence service applicable for ${config.datastore}")

        persistenceService.saveCollection(config.collection, dataToPersist)
    }
}