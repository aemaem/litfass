package lit.fass.litfass.server.schedule.model

import lit.fass.litfass.server.config.yaml.model.CollectionConfig
import lit.fass.litfass.server.retention.RetentionService
import org.quartz.Job
import org.quartz.JobExecutionContext
import org.slf4j.LoggerFactory

/**
 * @author Michael Mair
 */
class QuartzRetentionJob : Job {
    companion object : QuartzJob() {
        private val log = LoggerFactory.getLogger(this::class.java.enclosingClass)
        override fun getType(): String = "retention"
    }

    override fun execute(context: JobExecutionContext?) {
        val retentionService = context!!.mergedJobDataMap["retentionService"] as RetentionService
        val collectionConfig = context.mergedJobDataMap["collectionConfig"] as CollectionConfig
        retentionService.clean(collectionConfig)
        log.trace("Executed retention job ${collectionConfig.collection}")
    }
}