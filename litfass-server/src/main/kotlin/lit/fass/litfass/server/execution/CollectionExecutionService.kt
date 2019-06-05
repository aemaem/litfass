package lit.fass.litfass.server.execution

import lit.fass.litfass.server.config.yaml.model.CollectionConfig
import lit.fass.litfass.server.flow.FlowService
import lit.fass.litfass.server.persistence.CollectionPersistenceService
import lit.fass.litfass.server.persistence.CollectionPersistenceService.Companion.ID_KEY
import org.springframework.stereotype.Service

/**
 * @author Michael Mair
 */
@Service
class CollectionExecutionService(
    private val flowService: FlowService,
    private val collectionPersistenceServices: List<CollectionPersistenceService>
) : ExecutionService {

    override fun execute(config: CollectionConfig, data: Map<String, Any?>) {
        val dataToPersist = flowService.execute(data, config)
        val persistenceService = collectionPersistenceServices.find { it.isApplicable(config.datastore) }
            ?: throw ExecutionException("No persistence service applicable for ${config.datastore}")

        if (dataToPersist.containsKey(ID_KEY)) {
            persistenceService.saveCollection(config.collection, dataToPersist, dataToPersist[ID_KEY])
        } else {
            persistenceService.saveCollection(config.collection, dataToPersist)
        }
    }
}