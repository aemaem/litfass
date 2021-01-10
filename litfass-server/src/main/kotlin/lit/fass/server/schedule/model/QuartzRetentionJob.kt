package lit.fass.server.schedule.model

import lit.fass.server.config.yaml.model.CollectionConfig
import lit.fass.server.logger
import lit.fass.server.retention.RetentionService
import org.quartz.Job
import org.quartz.JobExecutionContext

/**
 * @author Michael Mair
 */
class QuartzRetentionJob : Job {
    companion object : QuartzJob() {
        private val log = this.logger()
        override fun getType(): String = "retention"
    }

    override fun execute(context: JobExecutionContext?) {
        val retentionService = context!!.mergedJobDataMap["retentionService"] as RetentionService
        val collectionConfig = context.mergedJobDataMap["collectionConfig"] as CollectionConfig
        retentionService.clean(collectionConfig)
        log.trace("Executed retention job ${collectionConfig.collection}")
    }
}