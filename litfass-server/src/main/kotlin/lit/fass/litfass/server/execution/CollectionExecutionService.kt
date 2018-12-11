package lit.fass.litfass.server.execution

import lit.fass.litfass.server.config.ConfigService
import lit.fass.litfass.server.flow.FlowService
import lit.fass.litfass.server.persistence.PersistenceService
import lit.fass.litfass.server.persistence.PersistenceService.Companion.ID_KEY

/**
 * @author Michael Mair
 */
class CollectionExecutionService(
    private val configService: ConfigService,
    private val flowService: FlowService,
    private val persistenceService: PersistenceService
) : ExecutionService {

    override fun execute(collection: String, data: Map<String, Any?>) {
        val config = configService.getConfig(collection)
        val dataToPersist = flowService.execute(data, config)
        if (dataToPersist.containsKey(ID_KEY)) {
            persistenceService.save(collection, dataToPersist[ID_KEY], dataToPersist)
        } else {
            persistenceService.save(collection, dataToPersist)
        }
    }
}