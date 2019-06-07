package lit.fass.litfass.server.rest

import lit.fass.litfass.server.config.ConfigService
import lit.fass.litfass.server.execution.ExecutionService
import lit.fass.litfass.server.persistence.CollectionPersistenceService
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR
import org.springframework.web.bind.annotation.*
import org.springframework.web.reactive.function.BodyInserters.fromObject
import org.springframework.web.reactive.function.server.ServerRequest
import org.springframework.web.reactive.function.server.ServerResponse
import org.springframework.web.reactive.function.server.ServerResponse.*
import reactor.core.publisher.Mono
import java.security.Principal
import java.time.OffsetDateTime
import java.time.ZoneOffset

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

    @PostMapping("/{collection}")
    fun addCollection(
        @PathVariable collection: String,
        @RequestBody body: Map<String, Any?>,
        request: ServerRequest
    ): Mono<ServerResponse> {

        if (collection.isBlank()) {
            return badRequest()
                .body(fromObject(mapOf("error" to "Collection must not be blank")))
        }

        val collectionHeaders = request.headers().asHttpHeaders().entries
            .associateBy({ entry -> entry.key }, { entry -> entry.value.joinToString(",") })
        log.trace("Got headers $collectionHeaders for collection $collection")
        val collectionMetaData = request.queryParams().entries
            .associateBy({ entry -> entry.key }, { entry -> entry.value.joinToString(",") })
        log.trace("Got collection $collection and meta data $collectionMetaData")

        val data = mutableMapOf<String, Any?>("timestamp" to OffsetDateTime.now(ZoneOffset.UTC))
        data.putAll(collectionHeaders)
        data.putAll(collectionMetaData)
        data.putAll(body)

        try {
            executionService.execute(configService.getConfig(collection), data)
        } catch (ex: Exception) {
            log.error("Exception during execution of collection $collection", ex)
            return status(INTERNAL_SERVER_ERROR).body(fromObject(mapOf("error" to ex.message)))
        }

        return ok().build()
    }

    @GetMapping("/{collection}/{id}")
    fun getCollection(@PathVariable collection: String, @PathVariable id: String, principal: Principal): Mono<ServerResponse> {

        if (collection.isBlank()) {
            return badRequest()
                .body(fromObject(mapOf("error" to "Collection must not be blank")))
        }
        if (id.isBlank()) {
            return badRequest()
                .body(fromObject(mapOf("error" to "Id must not be blank")))
        }


        val config = configService.getConfig(collection)
        val persistenceService = persistenceServices.find { it.isApplicable(config.datastore) }
        if (persistenceService == null) {
            return badRequest()
                .body(fromObject(mapOf("error" to "Persistence service for ${config.datastore} not found")))
        }

        log.debug("Getting collection data for $collection with id $id for user ${principal.name}")
        return ok().body(fromObject(persistenceService.findCollectionData(collection, id)))
    }
}
