package lit.fass.litfass.server.rest

import lit.fass.litfass.server.config.ConfigService
import lit.fass.litfass.server.execution.ExecutionService
import lit.fass.litfass.server.persistence.CollectionPersistenceService
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR
import org.springframework.http.MediaType.APPLICATION_JSON_UTF8_VALUE
import org.springframework.http.ResponseEntity
import org.springframework.http.ResponseEntity.*
import org.springframework.http.server.reactive.ServerHttpRequest
import org.springframework.web.bind.annotation.*
import reactor.core.publisher.Mono
import reactor.core.publisher.Mono.just
import java.security.Principal
import java.time.OffsetDateTime.now
import java.time.ZoneOffset.UTC

/**
 * @author Michael Mair
 */
@RestController
@RequestMapping("/collections")
class CollectionsController(
    private val executionService: ExecutionService,
    private val configService: ConfigService,
    private val persistenceServices: List<CollectionPersistenceService>
) {

    companion object {
        private val log = LoggerFactory.getLogger(this::class.java.enclosingClass)
    }

    @PostMapping("/{collection}", consumes = [APPLICATION_JSON_UTF8_VALUE])
    fun addCollection(
        @PathVariable collection: String,
        @RequestBody body: Map<String, Any?>,
        request: ServerHttpRequest
    ): Mono<ResponseEntity<Map<String, Any?>>> {

        if (collection.isBlank()) {
            return just(badRequest().body(mapOf<String, Any?>("error" to "Collection must not be blank")))
        }

        val collectionHeaders = request.headers.entries
            .associateBy({ entry -> entry.key }, { entry -> entry.value.joinToString(",") })
        log.trace("Got headers $collectionHeaders for collection $collection")
        val collectionMetaData = request.queryParams.entries
            .associateBy({ entry -> entry.key }, { entry -> entry.value.joinToString(",") })
        log.trace("Got collection $collection and meta data $collectionMetaData")

        val data = mutableMapOf<String, Any?>("timestamp" to now(UTC))
        data.putAll(collectionHeaders)
        data.putAll(collectionMetaData)
        data.putAll(body)

        try {
            executionService.execute(configService.getConfig(collection), data)
        } catch (ex: Exception) {
            log.error("Exception during execution of collection $collection", ex)
            return just(status(INTERNAL_SERVER_ERROR).body(mapOf<String, Any?>("error" to ex.message)))
        }

        return just(ok().build())
    }

    @GetMapping("/{collection}/{id}")
    fun getCollection(@PathVariable collection: String, @PathVariable id: String, principal: Principal): Mono<ResponseEntity<Map<String, Any?>>> {

        if (collection.isBlank()) {
            return just(
                badRequest()
                    .body(mapOf<String, Any?>("error" to "Collection must not be blank"))
            )
        }
        if (id.isBlank()) {
            return just(
                badRequest()
                    .body(mapOf<String, Any?>("error" to "Id must not be blank"))
            )
        }

        val config = configService.getConfig(collection)
        val persistenceService = persistenceServices.find { it.isApplicable(config.datastore) }
        if (persistenceService == null) {
            return just(
                badRequest()
                    .body(mapOf<String, Any?>("error" to "Persistence service for ${config.datastore} not found"))
            )
        }

        log.debug("Getting collection data for $collection with id $id for user ${principal.name}")
        return just(ok().body(persistenceService.findCollectionData(collection, id)))
    }
}
