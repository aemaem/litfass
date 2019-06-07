package lit.fass.litfass.server.rest

import lit.fass.litfass.server.config.ConfigService
import lit.fass.litfass.server.config.yaml.model.CollectionConfig
import org.slf4j.LoggerFactory
import org.springframework.http.MediaType.TEXT_PLAIN_VALUE
import org.springframework.http.ResponseEntity
import org.springframework.http.ResponseEntity.*
import org.springframework.web.bind.annotation.*
import reactor.core.publisher.Mono
import reactor.core.publisher.Mono.just
import java.io.ByteArrayInputStream
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
    fun getConfigs(principal: Principal): Mono<ResponseEntity<Collection<CollectionConfig>>> {
        // todo: implement pagination
        log.debug("Getting all configs for user ${principal.name}")
        return just(ok().body((configService.getConfigs())))
    }

    @GetMapping("/{collection}")
    fun getConfig(@PathVariable collection: String, principal: Principal): Mono<Any> {
        if (collection.isBlank()) {
            return just(badRequest().body(mapOf<String, Any?>("error" to "Collection must not be blank")))
        }
        log.debug("Getting config $collection for user ${principal.name}")
        return just(ok().body(configService.getConfig(collection)))
    }

    @DeleteMapping("/{collection}")
    fun deleteConfig(@PathVariable collection: String, principal: Principal): Mono<Any> {
        if (collection.isBlank()) {
            return just(badRequest().body(mapOf("error" to "Collection must not be blank")))
        }

        log.debug("Removing config $collection for user ${principal.name}")
        configService.removeConfig(collection)
        return just(noContent().build<Any>())
    }

    @PostMapping(consumes = [TEXT_PLAIN_VALUE])
    fun addConfig(@RequestBody body: String, principal: Principal): Mono<Any> {
        try {
            configService.readConfig(ByteArrayInputStream(body.toByteArray()))
        } catch (ex: Exception) {
            log.error("Unable to read config", ex)
            return just(badRequest().body(mapOf("error" to "Unable to read config: ${ex.message}")))
        }
        return just(noContent().build<Any>())
    }
}
