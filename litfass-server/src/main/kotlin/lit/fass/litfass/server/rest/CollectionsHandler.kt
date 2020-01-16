package lit.fass.litfass.server.rest

import lit.fass.litfass.server.config.ConfigService
import lit.fass.litfass.server.execution.ExecutionService
import lit.fass.litfass.server.persistence.CollectionPersistenceService
import org.slf4j.LoggerFactory
import org.springframework.core.ParameterizedTypeReference
import org.springframework.http.HttpMethod.GET
import org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.BodyInserters.fromObject
import org.springframework.web.reactive.function.server.ServerRequest
import org.springframework.web.reactive.function.server.ServerResponse
import org.springframework.web.reactive.function.server.ServerResponse.*
import reactor.core.publisher.Mono
import reactor.core.publisher.Mono.just
import java.time.OffsetDateTime.now
import java.time.ZoneOffset.UTC

/**
 * @author Michael Mair
 */
@Component
class CollectionsHandler(
    private val executionService: ExecutionService,
    private val configService: ConfigService,
    private val persistenceServices: List<CollectionPersistenceService>
) {

    companion object {
        private val log = LoggerFactory.getLogger(this::class.java.enclosingClass)
    }

    fun addCollection(request: ServerRequest): Mono<ServerResponse> {
        val collection = request.pathVariable("collection")
        if (collection.isBlank()) {
            return badRequest().body(fromObject(mapOf("error" to "Collection must not be blank")))
        }

        val collectionHeaders = request.headers().asHttpHeaders().entries
            .associateBy({ entry -> entry.key }, { entry -> entry.value.joinToString(",") })
        log.trace("Got headers $collectionHeaders for collection $collection")
        val collectionMetaData = request.queryParams().entries
            .associateBy({ entry -> entry.key }, { entry -> entry.value.joinToString(",") })
        log.trace("Got collection $collection and meta data $collectionMetaData")

        val data = mutableMapOf<String, Any?>("timestamp" to now(UTC))
        data.putAll(collectionHeaders)
        data.putAll(collectionMetaData)

        if (request.method() == GET) {
            try {
                executionService.execute(configService.getConfig(collection), data)
            } catch (ex: Exception) {
                log.error("Exception during execution of collection $collection", ex)
                return status(INTERNAL_SERVER_ERROR).body(fromObject(mapOf("error" to ex.message)))
            }
            return ok().build()
        }

        return request.bodyToMono(object : ParameterizedTypeReference<Map<String, Any?>>() {})
            .flatMap {
                data.putAll(it)
                return@flatMap just(data)
            }
            .flatMap {
                try {
                    executionService.execute(configService.getConfig(collection), it)
                } catch (ex: Exception) {
                    log.error("Exception during execution of collection $collection", ex)
                    return@flatMap status(INTERNAL_SERVER_ERROR).body(fromObject(mapOf("error" to ex.message)))
                }
                ok().build()
            }
    }

    fun getCollection(request: ServerRequest): Mono<ServerResponse> {
        val collection = request.pathVariable("collection")
        if (collection.isBlank()) {
            return badRequest().body(fromObject(mapOf("error" to "Collection must not be blank")))
        }
        val id = request.pathVariable("id")
        if (id.isBlank()) {
            return badRequest().body(fromObject(mapOf("error" to "Id must not be blank")))
        }

        val config = configService.getConfig(collection)
        val persistenceService = persistenceServices.find { it.isApplicable(config.datastore) }
        if (persistenceService == null) {
            return badRequest().body(fromObject(mapOf("error" to "Persistence service for ${config.datastore} not found")))
        }

        return request.principal().flatMap { principal ->
            log.debug("Getting collection data for $collection with id $id for user ${principal.name}")
            ok().body(fromObject(persistenceService.findCollectionData(collection, id)))
        }
    }
}
