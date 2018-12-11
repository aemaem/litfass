package lit.fass.litfass.server.schedule

import com.cronutils.descriptor.CronDescriptor
import com.cronutils.model.Cron
import com.cronutils.model.definition.CronDefinitionBuilder
import com.cronutils.model.time.ExecutionTime
import com.cronutils.parser.CronParser
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.channels.actor
import kotlinx.coroutines.channels.consumeEach
import kotlinx.coroutines.channels.sendBlocking
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import lit.fass.litfass.server.execution.ExecutionService
import lit.fass.litfass.server.schedule.model.CancelJobMessage
import lit.fass.litfass.server.schedule.model.CreateJobMessage
import lit.fass.litfass.server.schedule.model.ScheduledJobMessage
import lit.fass.litfass.server.schedule.model.SchedulerJob
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
class CollectionSchedulerService(private val executionService: ExecutionService) : SchedulerService {
    companion object {
        private val log = LoggerFactory.getLogger(this::class.java.enclosingClass)
        private val cronDefinition = CronDefinitionBuilder.defineCron()
            .withSeconds().and()
            .withMinutes().and()
            .withHours().and()
            .withDayOfMonth().and()
            .withMonth().and()
            .withDayOfWeek().withValidRange(0, 7).withMondayDoWValue(1).withIntMapping(7, 0).and()
            .withYear().and()
            .enforceStrictRanges()
            .instance()
        private val cronParser = CronParser(cronDefinition)
        private val cronDescriptor = CronDescriptor.instance()
    }

    private lateinit var scheduler: SendChannel<ScheduledJobMessage>

    init {
        GlobalScope.launch {
            scheduler = actor(capacity = 3) {
                val jobs: MutableMap<String, SchedulerJob> = mutableMapOf()
                consumeEach { message ->
                    when (message) {
                        is CreateJobMessage -> {
                            if (jobs.containsKey(message.name)) {
                                log.warn("Scheduled job ${message.name} already exists. Job won't be scheduled")
                                return@consumeEach
                            }
                            jobs[message.name] = SchedulerJob(
                                message.cron,
                                launch(coroutineContext) jobLaunch@{
                                    while (true) {
                                        log.trace("Executing job ${message.name}")
                                        val executionDelay =
                                            determineDelayForNextExecution(message.cron, ZonedDateTime.now(UTC))
                                        if (executionDelay < 0) {
                                            cancel()
                                            log.info("Job ${message.name} cancelled because there is no upcoming execution")
                                            return@jobLaunch
                                        } else {
                                            delay(executionDelay)
                                            executionService.execute(
                                                message.name,
                                                mapOf<String, Any?>("timestamp" to OffsetDateTime.now(UTC))
                                            )
                                            log.trace("Executed job ${message.name}")
                                        }
                                    }
                                })
                            log.info("Scheduled job ${message.name}")
                        }
                        is CancelJobMessage -> {
                            jobs[message.name]?.job?.cancel()
                            log.info("Cancelled scheduled job ${message.name}")
                        }
                    }
                }
            }
        }
    }

    override fun createJob(collection: String, cronExpression: String) {
        log.info("Creating scheduled job $collection with cron $cronExpression")
        val cron: Cron
        try {
            cron = cronParser.parse(cronExpression)
        } catch (ex: Exception) {
            throw SchedulerException("Unable to parse cron expression: ${ex.message}")
        }

        log.info("Sending job $collection to be scheduled ${cronDescriptor.describe(cron)}")
        scheduler.sendBlocking(CreateJobMessage(collection, cron))
    }

    override fun cancelJob(collection: String) {
        log.info("Sending job $collection to be cancelled")
        scheduler.sendBlocking(CancelJobMessage(collection))
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
