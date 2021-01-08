package lit.fass.server.retention

import lit.fass.server.config.yaml.model.CollectionConfig

/**
 * @author Michael Mair
 */
interface RetentionService {

    fun clean(config: CollectionConfig)
    fun setCronExpression(cron: String)
    fun getCronExpression(): String
}