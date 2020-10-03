package lit.fass.server.http.route

import akka.http.javadsl.marshallers.jackson.Jackson.marshaller
import akka.http.javadsl.marshallers.jackson.Jackson.unmarshaller
import akka.http.javadsl.model.StatusCodes.*
import akka.http.javadsl.server.PathMatchers.segment
import akka.http.javadsl.server.Route
import lit.fass.server.config.ConfigService
import lit.fass.server.execution.ExecutionService
import lit.fass.server.http.SecurityDirectives
import lit.fass.server.logger
import lit.fass.server.persistence.CollectionPersistenceService
import lit.fass.server.security.Role.ADMIN
import lit.fass.server.security.Role.READER
import lit.fass.server.security.SecurityManager
import org.apache.shiro.subject.Subject
import java.time.OffsetDateTime.now
import java.time.ZoneOffset.UTC

/**
 * @author Michael Mair
 */
class CollectionRoutes(
    securityManager: SecurityManager,
    private val configService: ConfigService,
    private val executionService: ExecutionService,
    private val persistenceServices: List<CollectionPersistenceService>
) : SecurityDirectives(securityManager) {

    companion object {
        private val log = this.logger()
    }

    val routes: Route = pathPrefix("collections") {
        path(segment()) { collection ->
            concat(
                extractRequest { request ->
                    val headers = request.headers.associateBy({ entry -> entry.name() }, { entry -> entry.value() })
                    parameterMap { queryParams ->
                        concat(
                            post {
                                entity(unmarshaller(Map::class.java)) { payload ->
                                    @Suppress("UNCHECKED_CAST")
                                    addCollection(collection, headers, queryParams, payload as Map<String, Any?>)
                                }
                            },
                            get {
                                addCollection(collection, headers, queryParams)
                            }
                        )
                    }
                },
                authenticate { subject ->
                    path(segment()) { id ->
                        get {
                            authorize(subject, listOf(ADMIN, READER)) {
                                getCollection(collection, id, subject)
                            }
                        }
                    }
                }
            )
        }
    }

    fun addCollection(
        collection: String,
        headers: Map<String, String?>,
        queryParams: Map<String, String?>,
        payload: Map<String, Any?> = emptyMap()
    ): Route {
        if (collection.isBlank()) {
            return complete(BAD_REQUEST, mapOf("error" to "Collection must not be blank"), marshaller())
        }

        log.trace("Got headers $headers for collection $collection")
        log.trace("Got collection $collection and meta data $queryParams")

        val data = mutableMapOf<String, Any?>("timestamp" to now(UTC))
        data.putAll(headers)
        data.putAll(queryParams)
        data.putAll(payload)

        try {
            executionService.execute(configService.getConfig(collection), listOf(data))
        } catch (ex: Exception) {
            log.error("Exception during execution of collection $collection", ex)
            return complete(INTERNAL_SERVER_ERROR, mapOf("error" to ex.message), marshaller())
        }
        return complete(OK)
    }

    private fun getCollection(collection: String, id: String, subject: Subject): Route {
        if (collection.isBlank()) {
            return complete(BAD_REQUEST, mapOf("error" to "Collection must not be blank"), marshaller())
        }
        if (id.isBlank()) {
            return complete(BAD_REQUEST, mapOf("error" to "Id must not be blank"), marshaller())
        }

        val config = configService.getConfig(collection)
        val persistenceService = persistenceServices.find { it.isApplicable(config.datastore) }
            ?: return complete(BAD_REQUEST, mapOf("error" to "Persistence service for ${config.datastore} not found"), marshaller())

        log.debug("Getting collection data for $collection with id $id for user ${subject.principal}")
        return complete(OK, persistenceService.findCollectionData(collection, id), marshaller())
    }
}