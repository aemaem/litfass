package lit.fass.server.actor

import akka.actor.typed.ActorRef
import akka.actor.typed.Behavior
import akka.actor.typed.javadsl.AbstractBehavior
import akka.actor.typed.javadsl.ActorContext
import akka.actor.typed.javadsl.Behaviors
import akka.actor.typed.javadsl.Behaviors.same
import akka.actor.typed.javadsl.Receive
import lit.fass.server.config.yaml.ConfigException
import lit.fass.server.config.yaml.model.CollectionConfig
import lit.fass.server.logger
import lit.fass.server.schedule.SchedulerService


/**
 * @author Michael Mair
 */
class SchedulerActor private constructor(
    private val schedulerService: SchedulerService,
    context: ActorContext<Message>?
) : AbstractBehavior<SchedulerActor.Message>(context) {

    companion object {
        private val log = this.logger()

        @JvmStatic
        fun create(schedulingService: SchedulerService): Behavior<Message> {
            return Behaviors.setup { context -> SchedulerActor(schedulingService, context) }
        }
    }

    interface Message : SerializationMarker
    class Done : Message
    data class ScheduleJob(val config: CollectionConfig, val replyTo: ActorRef<Done>) : Message
    data class CancelJob(val config: CollectionConfig, val replyTo: ActorRef<Done>) : Message


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