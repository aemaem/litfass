package lit.fass.server.http.route

import akka.http.javadsl.model.StatusCodes
import akka.http.javadsl.server.PathMatchers.segment
import akka.http.javadsl.server.Route
import lit.fass.server.config.ConfigService
import lit.fass.server.execution.ExecutionService
import lit.fass.server.http.SecurityDirectives
import lit.fass.server.logger
import lit.fass.server.security.SecurityManager

/**
 * @author Michael Mair
 */
class CollectionRoutes(
    securityManager: SecurityManager,
    private val configService: ConfigService,
    private val executionService: ExecutionService
) : SecurityDirectives(securityManager) {

    companion object {
        private val log = this.logger()
    }

    val routes: Route = pathPrefix("collections") {
        path(segment()) { collection ->
            concat(
                post {
                    complete(StatusCodes.NOT_IMPLEMENTED)
                },
                get {
                    complete(StatusCodes.NOT_IMPLEMENTED)
                },
                path(segment()) { id ->
                    get {
                        complete(StatusCodes.NOT_IMPLEMENTED)
                    }
                }
            )
        }
    }

    //todo: implement
//    fun addCollection(request: ServerRequest): Mono<ServerResponse> {
//        val collection = request.pathVariable("collection")
//        if (collection.isBlank()) {
//            return badRequest().body(fromValue(mapOf("error" to "Collection must not be blank")))
//        }
//
//        val collectionHeaders = request.headers().asHttpHeaders().entries
//            .associateBy({ entry -> entry.key }, { entry -> entry.value.joinToString(",") })
//        log.trace("Got headers $collectionHeaders for collection $collection")
//        val collectionMetaData = request.queryParams().entries
//            .associateBy({ entry -> entry.key }, { entry -> entry.value.joinToString(",") })
//        log.trace("Got collection $collection and meta data $collectionMetaData")
//
//        val data = mutableMapOf<String, Any?>("timestamp" to now(UTC))
//        data.putAll(collectionHeaders)
//        data.putAll(collectionMetaData)
//
//        if (request.method() == GET) {
//            try {
//                executionService.execute(configService.getConfig(collection), listOf(data))
//            } catch (ex: Exception) {
//                log.error("Exception during execution of collection $collection", ex)
//                return status(INTERNAL_SERVER_ERROR).body(fromValue(mapOf("error" to ex.message)))
//            }
//            return ok().build()
//        }
//
//        return request.bodyToMono(object : ParameterizedTypeReference<Map<String, Any?>>() {})
//            .flatMap {
//                data.putAll(it)
//                return@flatMap just(data)
//            }
//            .flatMap {
//                try {
//                    executionService.execute(configService.getConfig(collection), listOf(it))
//                } catch (ex: Exception) {
//                    log.error("Exception during execution of collection $collection", ex)
//                    return@flatMap status(INTERNAL_SERVER_ERROR).body(fromValue(mapOf("error" to ex.message)))
//                }
//                ok().build()
//            }
//    }
//
//    fun getCollection(request: ServerRequest): Mono<ServerResponse> {
//        val collection = request.pathVariable("collection")
//        if (collection.isBlank()) {
//            return badRequest().body(fromValue(mapOf("error" to "Collection must not be blank")))
//        }
//        val id = request.pathVariable("id")
//        if (id.isBlank()) {
//            return badRequest().body(fromValue(mapOf("error" to "Id must not be blank")))
//        }
//
//        val config = configService.getConfig(collection)
//        val persistenceService = persistenceServices.find { it.isApplicable(config.datastore) }
//        if (persistenceService == null) {
//            return badRequest().body(fromValue(mapOf("error" to "Persistence service for ${config.datastore} not found")))
//        }
//
//        return request.principal().flatMap { principal ->
//            log.debug("Getting collection data for $collection with id $id for user ${principal.name}")
//            ok().body(fromValue(persistenceService.findCollectionData(collection, id)))
//        }
//    }

}