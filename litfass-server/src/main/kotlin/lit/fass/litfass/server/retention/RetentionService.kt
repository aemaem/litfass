package lit.fass.litfass.server.retention

import lit.fass.litfass.server.config.yaml.model.CollectionConfig

/**
 * @author Michael Mair
 */
interface RetentionService {

    fun clean(config: CollectionConfig)
    fun setCronExpression(cron: String)
    fun getCronExpression(): String
}