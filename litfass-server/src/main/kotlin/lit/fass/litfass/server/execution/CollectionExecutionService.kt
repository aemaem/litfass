package lit.fass.litfass.server.execution

import lit.fass.litfass.server.config.ConfigService
import lit.fass.litfass.server.flow.FlowService
import lit.fass.litfass.server.persistence.CollectionPersistenceService
import lit.fass.litfass.server.persistence.CollectionPersistenceService.Companion.ID_KEY

/**
 * @author Michael Mair
 */
class CollectionExecutionService(
    private val configService: ConfigService,
    private val flowService: FlowService,
    private val collectionPersistenceServices: List<CollectionPersistenceService>
) : ExecutionService {

    override fun execute(collection: String, data: Map<String, Any?>) {
        val config = configService.getConfig(collection)
        val dataToPersist = flowService.execute(data, config)
        val persistenceService = collectionPersistenceServices.find { it.isApplicable(config.datastore) }
            ?: throw ExecutionException("No persistence service applicable for ${config.datastore}")

        if (dataToPersist.containsKey(ID_KEY)) {
            persistenceService.saveCollection(collection, dataToPersist, dataToPersist[ID_KEY])
        } else {
            persistenceService.saveCollection(collection, dataToPersist)
        }
    }
}