package lit.fass.litfass.server.rest

import lit.fass.litfass.server.script.ScriptEngine
import lit.fass.litfass.server.script.ScriptLanguage
import org.slf4j.LoggerFactory
import org.springframework.core.ParameterizedTypeReference
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.BodyInserters.fromObject
import org.springframework.web.reactive.function.BodyInserters.fromValue
import org.springframework.web.reactive.function.server.ServerRequest
import org.springframework.web.reactive.function.server.ServerResponse
import org.springframework.web.reactive.function.server.ServerResponse.badRequest
import org.springframework.web.reactive.function.server.ServerResponse.ok
import reactor.core.publisher.Mono
import java.util.Collections.singletonList

/**
 * @author Michael Mair
 */
@Component
class ScriptHandler(private val scriptEngines: List<ScriptEngine>) {

    companion object {
        private val log = LoggerFactory.getLogger(this::class.java.enclosingClass)
    }

    fun testScript(request: ServerRequest): Mono<ServerResponse> {
        val language = request.pathVariable("language")
        val scriptLanguage: ScriptLanguage
        try {
            scriptLanguage = ScriptLanguage.valueOf(language.toUpperCase())
        } catch (ex: Exception) {
            return badRequest().body(fromValue(mapOf("error" to "Language must be one of ${ScriptLanguage.values().joinToString { it.name.toLowerCase() }}")))
        }

        return request.principal().flatMap { principal ->
            log.info("Trying $language script for user ${principal.name}")
            val scriptEngine = scriptEngines.find { it.isApplicable(scriptLanguage) }
            if (scriptEngine == null) {
                return@flatMap badRequest().body(fromValue(mapOf("error" to "No script engine available for language $language")))
            }

            request.bodyToMono(object : ParameterizedTypeReference<Map<String, Any?>>() {}).flatMap { body ->
                val script = body["script"] as String
                @Suppress("UNCHECKED_CAST")
                val data = body["data"] as Map<String, Any?>
                try {
                    val result = scriptEngine.invoke(script, singletonList(data))
                    ok().body(fromValue(result))
                } catch (ex: Exception) {
                    badRequest().body(fromValue(mapOf("error" to ex.message)))
                }
            }
        }
    }
}
