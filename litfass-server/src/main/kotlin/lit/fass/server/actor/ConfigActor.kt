package lit.fass.server.actor

import akka.actor.typed.ActorRef
import akka.actor.typed.Behavior
import akka.actor.typed.javadsl.AbstractBehavior
import akka.actor.typed.javadsl.ActorContext
import akka.actor.typed.javadsl.Behaviors
import akka.actor.typed.javadsl.Behaviors.same
import akka.actor.typed.javadsl.Receive
import lit.fass.server.actor.ConfigActor.Message
import lit.fass.server.config.ConfigService
import lit.fass.server.config.yaml.model.CollectionConfig
import java.io.InputStream


/**
 * @author Michael Mair
 */
class ConfigActor private constructor(
    private val configService: ConfigService,
    context: ActorContext<Message>?
) : AbstractBehavior<Message>(context) {

    companion object {
        @JvmStatic
        fun create(configService: ConfigService): Behavior<Message> {
            return Behaviors.setup<Message> { context -> ConfigActor(configService, context) }
        }
    }

    interface Message
    class Done : Message
    data class GetConfig(val collection: String, val replyTo: ActorRef<Config>) : Message
    data class Config(val collection: String, val response: CollectionConfig) : Message
    data class GetConfigs(val replyTo: ActorRef<Configs>) : Message
    data class Configs(val response: Collection<CollectionConfig>) : Message
    data class AddConfig(val inputStream: InputStream, val replyTo: ActorRef<Done>) : Message
    data class RemoveConfig(val collection: String, val replyTo: ActorRef<Done>) : Message

    override fun createReceive(): Receive<Message> = newReceiveBuilder()
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
            it.replyTo.tell(Done())
            same()
        }
        .onMessage(RemoveConfig::class.java) {
            configService.removeConfig(it.collection)
            it.replyTo.tell(Done())
            same()
        }
        .build()

}