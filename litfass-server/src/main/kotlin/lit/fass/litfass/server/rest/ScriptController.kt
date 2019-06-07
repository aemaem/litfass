package lit.fass.litfass.server.rest

import lit.fass.litfass.server.script.ScriptEngine
import lit.fass.litfass.server.script.ScriptLanguage
import org.slf4j.LoggerFactory
import org.springframework.http.ResponseEntity
import org.springframework.http.ResponseEntity.badRequest
import org.springframework.http.ResponseEntity.ok
import org.springframework.web.bind.annotation.*
import reactor.core.publisher.Mono
import reactor.core.publisher.Mono.just
import java.security.Principal

/**
 * @author Michael Mair
 */
@RestController
@RequestMapping("/script")
class ScriptController(private val scriptEngines: List<ScriptEngine>) {

    companion object {
        private val log = LoggerFactory.getLogger(this::class.java.enclosingClass)
    }

    @PostMapping("/{language}/test")
    fun addConfig(@PathVariable language: String, @RequestBody body: Map<String, Any?>, principal: Principal): Mono<ResponseEntity<Map<String, Any?>>> {

        log.info("Trying $language script for user ${principal.name}")
        val scriptLanguage: ScriptLanguage
        try {
            scriptLanguage = ScriptLanguage.valueOf(language.toUpperCase())
        } catch (ex: Exception) {
            return just(badRequest().body(mapOf<String, Any?>("error" to "Language must be one of ${ScriptLanguage.values().joinToString { it.name.toLowerCase() }}")))
        }

        val scriptEngine = scriptEngines.find { it.isApplicable(scriptLanguage) }
        if (scriptEngine == null) {
            return just(badRequest().body(mapOf<String, Any?>("error" to "No script engine available for language $language")))
        }

        val script = body["script"] as String
        @Suppress("UNCHECKED_CAST")
        val data = body["data"] as Map<String, Any?>
        try {
            val result = scriptEngine.invoke(script, data)
            return just(ok().body(result))
        } catch (ex: Exception) {
            return just(badRequest().body(mapOf<String, Any?>("error" to ex.message)))
        }
    }
}
