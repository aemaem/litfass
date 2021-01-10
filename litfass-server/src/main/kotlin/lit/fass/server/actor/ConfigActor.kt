package lit.fass.server.actor

import akka.actor.typed.ActorRef
import akka.actor.typed.Behavior
import akka.actor.typed.Scheduler
import akka.actor.typed.javadsl.AbstractBehavior
import akka.actor.typed.javadsl.ActorContext
import akka.actor.typed.javadsl.AskPattern.ask
import akka.actor.typed.javadsl.Behaviors
import akka.actor.typed.javadsl.Behaviors.same
import akka.actor.typed.javadsl.Receive
import akka.japi.function.Function
import lit.fass.server.actor.ConfigActor.Message
import lit.fass.server.config.ConfigService
import lit.fass.server.config.yaml.model.CollectionConfig
import java.io.InputStream
import java.time.Duration


/**
 * @author Michael Mair
 */
class ConfigActor private constructor(
    private val schedulerActor: ActorRef<SchedulerActor.Message>,
    private val configService: ConfigService,
    context: ActorContext<Message>?,
    private val scheduler: Scheduler,
    private val timeout: Duration
) : AbstractBehavior<Message>(context) {

    companion object {
        @JvmStatic
        fun create(
            schedulerActor: ActorRef<SchedulerActor.Message>,
            configService: ConfigService,
            timeout: Duration
        ): Behavior<Message> {
            return Behaviors.setup { context ->
                ConfigActor(schedulerActor, configService, context, context.system.scheduler(), timeout)
            }
        }
    }

    interface Message : SerializationMarker
    class Done : Message
    class InitializeConfigs : Message
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
                    it.replyTo.tell(Done())
                }
            same()
        }
        .build()

}