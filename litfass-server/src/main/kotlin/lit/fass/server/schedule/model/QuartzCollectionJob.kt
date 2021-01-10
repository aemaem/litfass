package lit.fass.server.schedule.model

import lit.fass.server.config.yaml.model.CollectionConfig
import lit.fass.server.execution.ExecutionService
import lit.fass.server.logger
import org.quartz.Job
import org.quartz.JobExecutionContext
import java.time.OffsetDateTime
import java.time.ZoneOffset

/**
 * @author Michael Mair
 */
class QuartzCollectionJob : Job {
    companion object : QuartzJob() {
        private val log = this.logger()
        override fun getType(): String = "collection"
    }

    override fun execute(context: JobExecutionContext?) {
        val executionService = context!!.mergedJobDataMap["executionService"] as ExecutionService
        val collectionConfig = context.mergedJobDataMap["collectionConfig"] as CollectionConfig
        executionService.execute(
            collectionConfig,
            listOf(mapOf<String, Any?>("timestamp" to OffsetDateTime.now(ZoneOffset.UTC)))
        )
        log.trace("Executed collection job ${collectionConfig.collection}")
    }
}