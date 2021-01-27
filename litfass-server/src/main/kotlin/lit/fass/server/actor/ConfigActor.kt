package lit.fass.server.actor

import akka.actor.typed.*
import akka.actor.typed.javadsl.AbstractBehavior
import akka.actor.typed.javadsl.ActorContext
import akka.actor.typed.javadsl.AskPattern.ask
import akka.actor.typed.javadsl.Behaviors.*
import akka.actor.typed.javadsl.Receive
import akka.actor.typed.pubsub.Topic
import akka.japi.function.Function
import com.fasterxml.jackson.annotation.JsonCreator
import lit.fass.server.actor.ConfigActor.Message
import lit.fass.server.config.ConfigService
import lit.fass.server.config.yaml.model.CollectionConfig
import lit.fass.server.logger
import java.io.InputStream
import java.time.Duration


/**
 * @author Michael Mair
 */
class ConfigActor private constructor(
    private val schedulerActor: ActorRef<SchedulerActor.Message>,
    private val collectionConfigTopic: ActorRef<Topic.Command<Message>>,
    private val configService: ConfigService,
    private val scheduler: Scheduler,
    private val timeout: Duration,
    context: ActorContext<Message>?
) : AbstractBehavior<Message>(context) {

    companion object {
        private val log = this.logger()

        @JvmStatic
        fun create(
            schedulerActor: ActorRef<SchedulerActor.Message>,
            collectionConfigTopic: ActorRef<Topic.Command<Message>>,
            configService: ConfigService,
            timeout: Duration
        ): Behavior<Message> {
            return supervise<Message?>(setup { context ->
                ConfigActor(schedulerActor, collectionConfigTopic, configService, context.system.scheduler(), timeout, context)
            }).onFailure(SupervisorStrategy.restart())
        }
    }

    interface Message : SerializationMarker
    class Done : Message
    class InitializeConfigs : Message
    data class InvalidateConfig @JsonCreator constructor(val collection: String) : Message
    data class GetConfig(val collection: String, val replyTo: ActorRef<Config>) : Message
    data class Config(val collection: String, val response: CollectionConfig) : Message
    data class GetConfigs(val replyTo: ActorRef<Configs>) : Message
    data class Configs(val response: Collection<CollectionConfig>) : Message
    data class AddConfig(val inputStream: InputStream, val replyTo: ActorRef<Done>) : Message
    data class RemoveConfig(val collection: String, val replyTo: ActorRef<Done>) : Message


    override fun createReceive(): Receive<Message> = newReceiveBuilder()
        .onMessage(InitializeConfigs::class.java) {
            configService.initializeConfigs()
            same()
        }
        .onMessage(InvalidateConfig::class.java) {
            configService.invalidateConfig(it.collection)
            same()
        }
        .onMessage(GetConfig::class.java) {
            it.replyTo.tell(Config(it.collection, configService.getConfig(it.collection)))
            same()
        }
        .onMessage(GetConfigs::class.java) {
            it.replyTo.tell(Configs(configService.getConfigs()))
            same()
        }
        .onMessage(AddConfig::class.java) {
            configService.readConfig(it.inputStream)
                .forEach { config ->
                    ask(schedulerActor, Function<ActorRef<SchedulerActor.Done>, SchedulerActor.Message?> { ref ->
                        SchedulerActor.ScheduleJob(config, ref)
                    }, timeout, scheduler)
                        .thenRun {
                            collectionConfigTopic.tell(Topic.publish(InvalidateConfig(config.collection)))
                            it.replyTo.tell(Done())
                        }
                }
            same()
        }
        .onMessage(RemoveConfig::class.java) {
            val config = configService.getConfig(it.collection)
            ask(schedulerActor, Function<ActorRef<SchedulerActor.Done>, SchedulerActor.Message?> { ref ->
                SchedulerActor.CancelJob(config, ref)
            }, timeout, scheduler)
                .thenRun {
                    configService.removeConfig(it.collection)
                    collectionConfigTopic.tell(Topic.publish(InvalidateConfig(config.collection)))
                    it.replyTo.tell(Done())
                }
            same()
        }
        .onSignal(PreRestart::class.java) {
            log.info("Restarting actor")
            same()
        }
        .build()
}