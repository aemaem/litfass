package lit.fass.litfass.server.rest

import lit.fass.litfass.server.config.ConfigService
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.BodyInserters.fromObject
import org.springframework.web.reactive.function.BodyInserters.fromValue
import org.springframework.web.reactive.function.server.ServerRequest
import org.springframework.web.reactive.function.server.ServerResponse
import org.springframework.web.reactive.function.server.ServerResponse.*
import reactor.core.publisher.Mono
import java.io.ByteArrayInputStream

/**
 * @author Michael Mair
 */
@Component
class ConfigsHandler(private val configService: ConfigService) {

    companion object {
        private val log = LoggerFactory.getLogger(this::class.java.enclosingClass)
    }

    fun getConfigs(request: ServerRequest): Mono<ServerResponse> {
        // todo: implement pagination
        return request.principal().flatMap { principal ->
            log.debug("Getting all configs for user ${principal.name}")
            ok().body(fromValue(configService.getConfigs()))
        }
    }

    fun getConfig(request: ServerRequest): Mono<ServerResponse> {
        val collection = request.pathVariable("collection")
        if (collection.isBlank()) {
            return badRequest().body(fromValue(mapOf("error" to "Collection must not be blank")))
        }
        return request.principal().flatMap { principal ->
            log.debug("Getting config $collection for user ${principal.name}")
            ok().body(fromValue(configService.getConfig(collection)))
        }
    }

    fun deleteConfig(request: ServerRequest): Mono<ServerResponse> {
        val collection = request.pathVariable("collection")
        if (collection.isBlank()) {
            return badRequest().body(fromValue(mapOf("error" to "Collection must not be blank")))
        }
        return request.principal().flatMap { principal ->
            log.debug("Removing config $collection for user ${principal.name}")
            configService.removeConfig(collection)
            noContent().build()
        }
    }

    fun addConfig(request: ServerRequest): Mono<ServerResponse> {
        return request.bodyToMono(ByteArray::class.java).flatMap { body ->
            try {
                configService.readConfig(ByteArrayInputStream(body))
            } catch (ex: Exception) {
                log.error("Unable to read config", ex)
                return@flatMap badRequest().body(fromValue(mapOf("error" to "Unable to read config: ${ex.message}")))
            }
            noContent().build()
        }
    }
}
