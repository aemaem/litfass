package lit.fass.server.http.route

import akka.http.javadsl.model.StatusCodes
import akka.http.javadsl.server.PathMatchers.segment
import akka.http.javadsl.server.Route
import lit.fass.server.config.ConfigService
import lit.fass.server.http.SecurityDirectives
import lit.fass.server.logger
import lit.fass.server.security.SecurityManager

/**
 * @author Michael Mair
 */
class ConfigRoutes(
    securityManager: SecurityManager,
    private val configService: ConfigService
) : SecurityDirectives(securityManager) {

    companion object {
        private val log = this.logger()
    }

    val routes: Route = pathPrefix("configs") {
        concat(
            post {
                complete(StatusCodes.NOT_IMPLEMENTED)
            },
            get {
                complete(StatusCodes.NOT_IMPLEMENTED)
            },
            path(segment()) { collection ->
                concat(
                    get {
                        complete(StatusCodes.NOT_IMPLEMENTED)
                    },
                    delete {
                        complete(StatusCodes.NOT_IMPLEMENTED)
                    }
                )
            }
        )
    }

    //todo: implement
//    fun getConfigs(request: ServerRequest): Mono<ServerResponse> {
//        // todo: implement pagination
//        return request.principal().flatMap { principal ->
//            log.debug("Getting all configs for user ${principal.name}")
//            ok().body(fromValue(configService.getConfigs()))
//        }
//    }
//
//    fun getConfig(request: ServerRequest): Mono<ServerResponse> {
//        val collection = request.pathVariable("collection")
//        if (collection.isBlank()) {
//            return badRequest().body(fromValue(mapOf("error" to "Collection must not be blank")))
//        }
//        return request.principal().flatMap { principal ->
//            log.debug("Getting config $collection for user ${principal.name}")
//            ok().body(fromValue(configService.getConfig(collection)))
//        }
//    }
//
//    fun deleteConfig(request: ServerRequest): Mono<ServerResponse> {
//        val collection = request.pathVariable("collection")
//        if (collection.isBlank()) {
//            return badRequest().body(fromValue(mapOf("error" to "Collection must not be blank")))
//        }
//        return request.principal().flatMap { principal ->
//            log.debug("Removing config $collection for user ${principal.name}")
//            configService.removeConfig(collection)
//            noContent().build()
//        }
//    }
//
//    fun addConfig(request: ServerRequest): Mono<ServerResponse> {
//        return request.bodyToMono(ByteArray::class.java).flatMap { body ->
//            try {
//                configService.readConfig(ByteArrayInputStream(body))
//            } catch (ex: Exception) {
//                log.error("Unable to read config", ex)
//                return@flatMap badRequest().body(fromValue(mapOf("error" to "Unable to read config: ${ex.message}")))
//            }
//            noContent().build()
//        }
//    }

}