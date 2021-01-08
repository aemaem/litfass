package lit.fass.server.http.route

import akka.actor.typed.ActorRef
import akka.actor.typed.Scheduler
import akka.actor.typed.javadsl.AskPattern.ask
import akka.http.javadsl.marshallers.jackson.Jackson.marshaller
import akka.http.javadsl.marshallers.jackson.Jackson.unmarshaller
import akka.http.javadsl.model.StatusCodes.OK
import akka.http.javadsl.server.PathMatchers.segment
import akka.http.javadsl.server.Route
import akka.japi.function.Function
import lit.fass.server.CollectionException
import lit.fass.server.actor.CollectionActor
import lit.fass.server.actor.CollectionActor.*
import lit.fass.server.execution.ExecutionException
import lit.fass.server.http.SecurityDirectives
import lit.fass.server.logger
import lit.fass.server.security.Role.ADMIN
import lit.fass.server.security.Role.READER
import lit.fass.server.security.SecurityManager
import org.apache.shiro.subject.Subject
import java.time.Duration
import java.util.concurrent.CompletionStage

/**
 * @author Michael Mair
 */
class CollectionRoutes(
    securityManager: SecurityManager,
    private val collectionActor: ActorRef<Message>,
    private val scheduler: Scheduler,
    private val timeout: Duration
) : SecurityDirectives(securityManager) {

    companion object {
        private val log = this.logger()
    }

    val routes: Route = pathPrefix("collections") {
        concat(
            path(segment().slash(segment())) { collection, id ->
                authenticate { subject ->
                    get {
                        authorize(subject, listOf(ADMIN, READER)) {
                            onSuccess(getCollection(collection, id, subject)) { complete(OK, it.response, marshaller()) }
                        }
                    }
                }
            },
            path(segment()) { collection ->
                extractRequest { request ->
                    val headers = request.headers.associateBy({ entry -> entry.name() }, { entry -> entry.value() })
                    parameterMap { queryParams ->
                        concat(
                            post {
                                entity(unmarshaller(Map::class.java)) { payload ->
                                    @Suppress("UNCHECKED_CAST")
                                    onSuccess(executeCollection(collection, headers, queryParams, payload as Map<String, Any?>)) { complete(OK) }
                                }
                            },
                            get {
                                onSuccess(executeCollection(collection, headers, queryParams, emptyMap())) { complete(OK) }
                            }
                        )
                    }
                }
            }
        )
    }

    fun executeCollection(
        collection: String,
        headers: Map<String, String?>,
        queryParams: Map<String, String?>,
        payload: Map<String, Any?> = emptyMap()
    ): CompletionStage<Done> {
        if (collection.isBlank()) {
            throw ExecutionException("Collection must not be blank")
        }

        log.trace("Got headers $headers for collection $collection")
        log.trace("Got collection $collection and meta data $queryParams")

        return ask(collectionActor, Function<ActorRef<Done>, Message?> { ref ->
            ExecuteCollection(collection, headers, queryParams, payload, ref)
        }, timeout, scheduler)
    }

    private fun getCollection(collection: String, id: String, subject: Subject): CompletionStage<CollectionActor.Collection> {
        if (collection.isBlank()) {
            throw CollectionException("Collection must not be blank")
        }
        if (id.isBlank()) {
            throw CollectionException("Id must not be blank")
        }

        return ask(collectionActor, Function<ActorRef<CollectionActor.Collection>, Message?> { ref ->
            GetCollection(collection, id, subject, ref)
        }, timeout, scheduler)
    }

}