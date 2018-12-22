package lit.fass.litfass.server.schedule.model

import com.cronutils.model.Cron
import lit.fass.litfass.server.config.yaml.model.CollectionConfig

/**
 * @author Michael Mair
 */
sealed class ScheduledJobMessage

data class CreateCollectionJobMessage(val config: CollectionConfig, val cron: Cron) : ScheduledJobMessage()
data class CancelCollectionJobMessage(val config: CollectionConfig) : ScheduledJobMessage()

data class CreateRetentionJobMessage(val config: CollectionConfig, val cron: Cron) : ScheduledJobMessage()
data class CancelRetentionJobMessage(val config: CollectionConfig) : ScheduledJobMessage()
