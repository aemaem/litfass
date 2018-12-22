package lit.fass.litfass.server.retention

import lit.fass.litfass.server.config.yaml.model.CollectionConfig
import lit.fass.litfass.server.persistence.CollectionPersistenceService
import org.slf4j.LoggerFactory
import java.time.Duration
import java.time.OffsetDateTime.now
import java.time.ZoneOffset.UTC

/**
 * @author Michael Mair
 */
class CollectionRetentionService(
    private val retentionCronExpression: String,
    private val collectionPersistenceServices: List<CollectionPersistenceService>
) : RetentionService {
    companion object {
        private val log = LoggerFactory.getLogger(this::class.java.enclosingClass)
    }

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

    override fun getCronExpression(): String {
        return retentionCronExpression
    }
}