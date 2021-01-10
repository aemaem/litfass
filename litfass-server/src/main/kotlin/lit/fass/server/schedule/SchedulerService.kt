package lit.fass.server.schedule

import lit.fass.server.config.yaml.model.CollectionConfig

/**
 * @author Michael Mair
 */
interface SchedulerService {

    fun stop()
    fun createCollectionJob(config: CollectionConfig)
    fun cancelCollectionJob(config: CollectionConfig)
    fun createRetentionJob(config: CollectionConfig)
    fun cancelRetentionJob(config: CollectionConfig)
}