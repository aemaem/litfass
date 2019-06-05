package lit.fass.litfass.server.schedule

import com.cronutils.descriptor.CronDescriptor
import com.cronutils.model.Cron
import com.cronutils.model.CronType.QUARTZ
import com.cronutils.model.definition.CronDefinitionBuilder.instanceDefinitionFor
import com.cronutils.parser.CronParser
import lit.fass.litfass.server.config.yaml.model.CollectionConfig
import lit.fass.litfass.server.execution.ExecutionService
import lit.fass.litfass.server.retention.RetentionService
import lit.fass.litfass.server.schedule.model.QuartzCollectionJob
import lit.fass.litfass.server.schedule.model.QuartzRetentionJob
import org.quartz.CronScheduleBuilder.cronSchedule
import org.quartz.CronTrigger
import org.quartz.Job
import org.quartz.JobBuilder.newJob
import org.quartz.JobDataMap
import org.quartz.JobDetail
import org.quartz.TriggerBuilder.newTrigger
import org.quartz.TriggerKey.triggerKey
import org.quartz.impl.StdSchedulerFactory
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import javax.annotation.PreDestroy


/**
 * Scheduling service in order to schedule background tasks based on coroutines based on cron expressions with
 * definition of the Unix crontab with seconds and year (https://www.unix.com/man-page/linux/5/crontab).
 * Scheduling is based on UTC.
 *
 * @author Michael Mair
 */
@Service
class QuartzCollectionSchedulerService(
    private val executionService: ExecutionService,
    private val retentionService: RetentionService
) : SchedulerService {
    companion object {
        private val log = LoggerFactory.getLogger(this::class.java.enclosingClass)
        private val cronDefinition = instanceDefinitionFor(QUARTZ)
        private val cronParser = CronParser(cronDefinition)
        private val cronDescriptor = CronDescriptor.instance()
    }

    private var scheduler = StdSchedulerFactory.getDefaultScheduler()

    init {
        scheduler.start()
    }

    @PreDestroy
    fun stop() {
        scheduler.shutdown()
    }

    override fun createCollectionJob(config: CollectionConfig) {
        log.info("Creating scheduled collection job ${config.collection} with cron ${config.scheduled}")
        val cron: Cron
        try {
            cron = cronParser.parse(config.scheduled)
        } catch (ex: Exception) {
            throw SchedulerException("Unable to parse cron expression: ${ex.message}")
        }

        log.info("Collection job ${config.collection} to be scheduled ${cronDescriptor.describe(cron)}")
        val job = job(
            QuartzCollectionJob::class.java,
            mapOf("executionService" to executionService, "collectionConfig" to config)
        )
        if (scheduler.checkExists(triggerKey(config.collection, QuartzCollectionJob.getType()))) {
            cancelCollectionJob(config)
        }
        scheduler.scheduleJob(
            job,
            jobTrigger(
                config,
                config.scheduled!!,
                QuartzCollectionJob.getType(),
                job
            )
        )
    }

    override fun cancelCollectionJob(config: CollectionConfig) {
        log.info("Collection job ${config.collection} to be cancelled")
        scheduler.unscheduleJob(triggerKey(config.collection, QuartzCollectionJob.getType()))
    }

    override fun createRetentionJob(config: CollectionConfig) {
        log.info("Creating scheduled retention job ${config.collection} with cron ${retentionService.getCronExpression()}")
        val cron: Cron
        try {
            cron = cronParser.parse(retentionService.getCronExpression())
        } catch (ex: Exception) {
            throw SchedulerException("Unable to parse cron expression: ${ex.message}")
        }

        log.info("Retention job ${config.collection} to be scheduled ${cronDescriptor.describe(cron)}")
        val job = job(
            QuartzRetentionJob::class.java,
            mapOf("retentionService" to retentionService, "collectionConfig" to config)
        )
        if (scheduler.checkExists(triggerKey(config.collection, QuartzRetentionJob.getType()))) {
            cancelRetentionJob(config)
        }
        scheduler.scheduleJob(
            job,
            jobTrigger(
                config,
                retentionService.getCronExpression(),
                QuartzRetentionJob.getType(),
                job
            )
        )
    }

    override fun cancelRetentionJob(config: CollectionConfig) {
        log.info("Retention job ${config.collection} to be cancelled")
        scheduler.unscheduleJob(triggerKey(config.collection, QuartzRetentionJob.getType()))
    }

    private fun jobTrigger(config: CollectionConfig, cron: String, type: String, job: JobDetail): CronTrigger {
        return newTrigger()
            .withIdentity(triggerKey(config.collection, type))
            .forJob(job)
            .withSchedule(cronSchedule(cron))
            .startNow()
            .build()
    }

    private fun job(jobType: Class<out Job>, context: Map<String, Any>): JobDetail {
        return newJob(jobType)
            .withDescription("Collection job")
            .setJobData(JobDataMap(context))
            .build()
    }
}
