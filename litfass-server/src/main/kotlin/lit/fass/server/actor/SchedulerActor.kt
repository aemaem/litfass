package lit.fass.server.actor

import akka.actor.typed.ActorRef
import akka.actor.typed.Behavior
import akka.actor.typed.PostStop
import akka.actor.typed.PreRestart
import akka.actor.typed.javadsl.AbstractBehavior
import akka.actor.typed.javadsl.ActorContext
import akka.actor.typed.javadsl.Behaviors.same
import akka.actor.typed.javadsl.Behaviors.setup
import akka.actor.typed.javadsl.Receive
import lit.fass.server.config.ConfigService
import lit.fass.server.config.yaml.ConfigException
import lit.fass.server.config.yaml.model.CollectionConfig
import lit.fass.server.execution.ExecutionService
import lit.fass.server.logger
import lit.fass.server.retention.RetentionService
import lit.fass.server.schedule.QuartzCollectionSchedulerService
import lit.fass.server.schedule.SchedulerService


/**
 * @author Michael Mair
 */
class SchedulerActor private constructor(
    private val schedulerService: SchedulerService,
    configService: ConfigService,
    context: ActorContext<Message>?
) : AbstractBehavior<SchedulerActor.Message>(context) {

    companion object {
        private val log = this.logger()

        @JvmStatic
        fun create(executionService: ExecutionService, retentionService: RetentionService, configService: ConfigService): Behavior<Message> {
            return setup { context ->
                SchedulerActor(QuartzCollectionSchedulerService(executionService, retentionService), configService, context)
            }
        }
    }

    interface Message : SerializationMarker
    class Done : Message
    data class ScheduleJob(val config: CollectionConfig, val replyTo: ActorRef<Done>) : Message
    data class CancelJob(val config: CollectionConfig, val replyTo: ActorRef<Done>) : Message


    init {
        val configs = configService.getConfigs()
        log.debug("Initializing {} collection configs", configs.size)
        configs.forEach { scheduleConfig(it) }
        log.info("Initialized {} collection configs", configs.size)
    }

    override fun createReceive(): Receive<Message> = newReceiveBuilder()
        .onMessage(ScheduleJob::class.java) {
            scheduleConfig(it.config)
            it.replyTo.tell(Done())
            same()
        }
        .onMessage(CancelJob::class.java) {
            schedulerService.cancelCollectionJob(it.config)
            schedulerService.cancelRetentionJob(it.config)
            it.replyTo.tell(Done())
            same()
        }
        .onSignal(PreRestart::class.java) {
            log.info("Restarting actor")
            same()
        }
        .onSignal(PostStop::class.java) {
            log.info("Stopping actor")
            schedulerService.stop()
            same()
        }
        .build()

    private fun scheduleConfig(config: CollectionConfig) {
        if (config.scheduled != null) {
            try {
                schedulerService.createCollectionJob(config)
            } catch (ex: Exception) {
                log.error("Unable to schedule config ${config.collection}", ex)
                throw ConfigException("Unable to schedule collection config ${config.collection}: ${ex.message}")
            }
        }
        if (config.retention != null) {
            try {
                schedulerService.createRetentionJob(config)
            } catch (ex: Exception) {
                log.error("Unable to schedule config ${config.collection}", ex)
                throw ConfigException("Unable to schedule retention config ${config.collection}: ${ex.message}")
            }
        }
    }

}