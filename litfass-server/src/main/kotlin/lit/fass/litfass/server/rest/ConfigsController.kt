package lit.fass.litfass.server.rest

import lit.fass.litfass.server.config.ConfigService
import org.slf4j.LoggerFactory
import org.springframework.web.bind.annotation.*
import org.springframework.web.reactive.function.BodyInserters.fromObject
import org.springframework.web.reactive.function.server.ServerResponse
import org.springframework.web.reactive.function.server.ServerResponse.*
import reactor.core.publisher.Mono
import java.io.InputStream
import java.security.Principal

/**
 * @author Michael Mair
 */
@RestController
@RequestMapping("/configs")
class ConfigsController(private val configService: ConfigService) {

    companion object {
        private val log = LoggerFactory.getLogger(this::class.java.enclosingClass)
    }

    @GetMapping
    fun getConfigs(principal: Principal): Mono<ServerResponse> {
        // todo: implement pagination
        log.debug("Getting all configs for user ${principal.name}")
        return ok().body(fromObject(configService.getConfigs()))
    }

    @GetMapping("/{collection}")
    fun getConfig(@PathVariable collection: String, principal: Principal): Mono<ServerResponse> {
        if (collection.isBlank()) {
            return badRequest().body(fromObject(mapOf("error" to "Collection must not be blank")))
        }
        log.debug("Getting config $collection for user ${principal.name}")
        return ok().body(fromObject(configService.getConfig(collection)))
    }

    @DeleteMapping("/{collection}")
    fun deleteConfig(@PathVariable collection: String, principal: Principal): Mono<ServerResponse> {
        if (collection.isBlank()) {
            return badRequest().body(fromObject(mapOf("error" to "Collection must not be blank")))
        }

        log.debug("Removing config $collection for user ${principal.name}")
        configService.removeConfig(collection)
        return noContent().build()
    }

    @PostMapping
    fun addConfig(@RequestBody body: InputStream, principal: Principal): Mono<ServerResponse> {
        try {
            configService.readConfig(body)
        } catch (ex: Exception) {
            log.error("Unable to read config", ex)
            return badRequest().body(fromObject(mapOf("error" to "Unable to read config: ${ex.message}")))
        }
        return ok().build()
    }
}
