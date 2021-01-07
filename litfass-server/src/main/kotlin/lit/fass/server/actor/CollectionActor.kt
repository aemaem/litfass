package lit.fass.server.actor

import akka.actor.typed.ActorRef
import akka.actor.typed.Behavior
import akka.actor.typed.javadsl.AbstractBehavior
import akka.actor.typed.javadsl.ActorContext
import akka.actor.typed.javadsl.Behaviors
import akka.actor.typed.javadsl.Behaviors.same
import akka.actor.typed.javadsl.Receive
import lit.fass.server.actor.CollectionActor.Message
import lit.fass.server.config.ConfigService
import lit.fass.server.execution.ExecutionException
import lit.fass.server.execution.ExecutionService
import lit.fass.server.logger
import lit.fass.server.persistence.CollectionPersistenceService
import lit.fass.server.persistence.PersistenceException
import org.apache.shiro.subject.Subject
import java.time.OffsetDateTime
import java.time.ZoneOffset


/**
 * @author Michael Mair
 */
class CollectionActor private constructor(
    private val configService: ConfigService,
    private val executionService: ExecutionService,
    private val persistenceServices: List<CollectionPersistenceService>,
    context: ActorContext<Message>?
) : AbstractBehavior<Message>(context) {

    companion object {
        private val log = this.logger()

        @JvmStatic
        fun create(
            configService: ConfigService,
            executionService: ExecutionService,
            persistenceServices: List<CollectionPersistenceService>
        ): Behavior<Message> {
            return Behaviors.setup<Message> { context -> CollectionActor(configService, executionService, persistenceServices, context) }
        }
    }

    interface Message
    class Done : Message
    data class GetCollection(val collection: String, val id: String, val subject: Subject, val replyTo: ActorRef<Collection>) : Message
    data class Collection(val response: Map<String, Any?>) : Message
    data class ExecuteCollection(
        val collection: String,
        val headers: Map<String, String?>,
        val queryParams: Map<String, String?>,
        val payload: Map<String, Any?>,
        val replyTo: ActorRef<Done>
    ) : Message

    override fun createReceive(): Receive<Message> = newReceiveBuilder()
        .onMessage(GetCollection::class.java) { message ->
            val config = configService.getConfig(message.collection)
            val persistenceService = persistenceServices.find { it.isApplicable(config.datastore) }
                ?: throw PersistenceException("Persistence service for ${config.datastore} not found")
            log.debug("Getting collection data for ${message.collection} with id ${message.id} for user ${message.subject.principal}")

            message.replyTo.tell(Collection(persistenceService.findCollectionData(message.collection, message.id)))
            same()
        }
        .onMessage(ExecuteCollection::class.java) { message ->
            val data = mutableMapOf<String, Any?>("timestamp" to OffsetDateTime.now(ZoneOffset.UTC))
            data.putAll(message.headers)
            data.putAll(message.queryParams)
            data.putAll(message.payload)

            try {
                executionService.execute(configService.getConfig(message.collection), listOf(data))
            } catch (ex: Exception) {
                throw ExecutionException("Exception during execution of collection ${message.collection}: ${ex.message}")
            }
            message.replyTo.tell((Done()))
            same()
        }
        .build()

}