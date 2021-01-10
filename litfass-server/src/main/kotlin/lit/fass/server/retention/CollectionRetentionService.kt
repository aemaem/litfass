package lit.fass.server.retention

import lit.fass.server.config.yaml.model.CollectionConfig
import lit.fass.server.logger
import lit.fass.server.persistence.CollectionPersistenceService
import java.time.Duration
import java.time.OffsetDateTime.now
import java.time.ZoneOffset.UTC

/**
 * @author Michael Mair
 */
class CollectionRetentionService(
    private val collectionPersistenceServices: List<CollectionPersistenceService>
) : RetentionService {

    companion object {
        private val log = this.logger()
    }

    private var retentionCronExpression: String = "0 0 0 ? * SUN *"

    override fun clean(config: CollectionConfig) {
        val persistenceService = collectionPersistenceServices.find { it.isApplicable(config.datastore) }
            ?: throw RetentionException("No persistence service applicable for ${config.datastore}")

        if (config.retention == null) {
            log.warn("Collection config ${config.collection} does not have a retention duration defined")
            return
        }
        val duration = Duration.parse(config.retention)
        persistenceService.deleteBefore(config.collection, now(UTC).minus(duration))
    }

    override fun setCronExpression(cron: String) {
        retentionCronExpression = cron
    }

    override fun getCronExpression(): String {
        return retentionCronExpression
    }
}