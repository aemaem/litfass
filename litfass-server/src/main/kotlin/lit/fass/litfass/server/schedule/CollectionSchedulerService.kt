package lit.fass.litfass.server.schedule

import com.cronutils.descriptor.CronDescriptor
import com.cronutils.model.Cron
import com.cronutils.model.CronType.QUARTZ
import com.cronutils.model.definition.CronDefinitionBuilder.instanceDefinitionFor
import com.cronutils.model.time.ExecutionTime
import com.cronutils.parser.CronParser
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.channels.actor
import kotlinx.coroutines.channels.consumeEach
import kotlinx.coroutines.channels.sendBlocking
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import lit.fass.litfass.server.config.yaml.model.CollectionConfig
import lit.fass.litfass.server.execution.ExecutionService
import lit.fass.litfass.server.retention.RetentionService
import lit.fass.litfass.server.schedule.model.*
import org.slf4j.LoggerFactory
import java.time.OffsetDateTime
import java.time.ZoneOffset.UTC
import java.time.ZonedDateTime

/**
 * Scheduling service in order to schedule background tasks based on coroutines based on cron expressions with
 * definition of the Unix crontab with seconds and year (https://www.unix.com/man-page/linux/5/crontab).
 * Scheduling is based on UTC.
 *
 * @author Michael Mair
 */
@Suppress("EXPERIMENTAL_API_USAGE")
class CollectionSchedulerService(
    private val executionService: ExecutionService,
    private val retentionService: RetentionService
) : SchedulerService {
    companion object {
        private val log = LoggerFactory.getLogger(this::class.java.enclosingClass)
        private val cronDefinition = instanceDefinitionFor(QUARTZ)
        private val cronParser = CronParser(cronDefinition)
        private val cronDescriptor = CronDescriptor.instance()
    }

    private var scheduler: SendChannel<ScheduledJobMessage>? = null

    init {
        GlobalScope.launch {
            scheduler = actor(capacity = 3) {
                val collectionJobs: MutableMap<String, SchedulerJob> = mutableMapOf()
                val retentionJobs: MutableMap<String, SchedulerJob> = mutableMapOf()
                consumeEach { message ->
                    when (message) {
                        is CreateCollectionJobMessage -> {
                            if (collectionJobs.containsKey(message.config.collection)) {
                                log.warn("Scheduled collection job ${message.config.collection} already exists. Job won't be scheduled")
                                return@consumeEach
                            }
                            collectionJobs[message.config.collection] = SchedulerJob(
                                message.cron,
                                launch(coroutineContext) jobLaunch@{
                                    while (true) {
                                        log.trace("Executing collection job ${message.config.collection}")
                                        val executionDelay =
                                            determineDelayForNextExecution(message.cron, ZonedDateTime.now(UTC))
                                        if (executionDelay < 0) {
                                            cancel()
                                            log.info("Collection job ${message.config.collection} cancelled because there is no upcoming execution")
                                            return@jobLaunch
                                        } else {
                                            delay(executionDelay)
                                            executionService.execute(
                                                message.config,
                                                mapOf<String, Any?>("timestamp" to OffsetDateTime.now(UTC))
                                            )
                                            log.trace("Executed collection job ${message.config.collection}")
                                        }
                                    }
                                })
                            log.info("Scheduled collection job ${message.config.collection}")
                        }
                        is CancelCollectionJobMessage -> {
                            collectionJobs[message.config.collection]?.job?.cancel()
                            log.info("Cancelled scheduled collection job ${message.config.collection}")
                        }
                        is CreateRetentionJobMessage -> {
                            if (retentionJobs.containsKey(message.config.collection)) {
                                log.warn("Scheduled retention job ${message.config.collection} already exists. Job won't be scheduled")
                                return@consumeEach
                            }
                            retentionJobs[message.config.collection] = SchedulerJob(
                                message.cron,
                                launch(coroutineContext) jobLaunch@{
                                    while (true) {
                                        log.trace("Executing retention job ${message.config.collection}")
                                        val executionDelay =
                                            determineDelayForNextExecution(message.cron, ZonedDateTime.now(UTC))
                                        if (executionDelay < 0) {
                                            cancel()
                                            log.info("Retention job ${message.config.collection} cancelled because there is no upcoming execution")
                                            return@jobLaunch
                                        } else {
                                            delay(executionDelay)
                                            retentionService.clean(message.config)
                                            log.trace("Executed retention job ${message.config.collection}")
                                        }
                                    }
                                })
                            log.info("Scheduled retention job ${message.config.collection}")
                        }
                        is CancelRetentionJobMessage -> {
                            retentionJobs[message.config.collection]?.job?.cancel()
                            log.info("Cancelled scheduled retention job ${message.config.collection}")
                        }
                    }
                }
            }
        }
        log.debug("Waiting for scheduler to be initialized.")
        runBlocking {
            while (scheduler == null) {
                delay(10)
            }
            log.debug("Scheduler initialized")
        }
    }

    override fun createCollectionJob(config: CollectionConfig) {
        log.info("Creating scheduled collection job ${config.collection} with cron ${config.scheduled}")
        val cron: Cron
        try {
            cron = cronParser.parse(config.scheduled)
        } catch (ex: Exception) {
            throw SchedulerException("Unable to parse cron expression: ${ex.message}")
        }

        log.info("Sending collection job ${config.collection} to be scheduled ${cronDescriptor.describe(cron)}")
        scheduler?.sendBlocking(CreateCollectionJobMessage(config, cron))
    }

    override fun cancelCollectionJob(config: CollectionConfig) {
        log.info("Sending collection job ${config.collection} to be cancelled")
        scheduler?.sendBlocking(CancelCollectionJobMessage(config))
    }

    override fun createRetentionJob(config: CollectionConfig) {
        log.info("Creating scheduled retention job ${config.collection} with cron ${retentionService.getCronExpression()}")
        val cron: Cron
        try {
            cron = cronParser.parse(retentionService.getCronExpression())
        } catch (ex: Exception) {
            throw SchedulerException("Unable to parse cron expression: ${ex.message}")
        }

        log.info("Sending retention job ${config.collection} to be scheduled ${cronDescriptor.describe(cron)}")
        scheduler?.sendBlocking(CreateRetentionJobMessage(config, cron))
    }

    override fun cancelRetentionJob(config: CollectionConfig) {
        log.info("Sending retention job ${config.collection} to be cancelled")
        scheduler?.sendBlocking(CancelRetentionJobMessage(config))
    }

    /**
     * Determine delay based on the cron expression in milli seconds.
     */
    private fun determineDelayForNextExecution(cron: Cron, now: ZonedDateTime): Long {
        return ExecutionTime.forCron(cron).timeToNextExecution(now)
            .map { it.toMillis() }
            .orElse(-1)
    }
}
