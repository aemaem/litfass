package lit.fass.server.http.route

import akka.http.javadsl.marshallers.jackson.Jackson.marshaller
import akka.http.javadsl.marshallers.jackson.Jackson.unmarshaller
import akka.http.javadsl.model.StatusCodes.BAD_REQUEST
import akka.http.javadsl.model.StatusCodes.OK
import akka.http.javadsl.server.PathMatchers.segment
import akka.http.javadsl.server.Route
import lit.fass.server.http.SecurityDirectives
import lit.fass.server.logger
import lit.fass.server.script.ScriptEngine
import lit.fass.server.script.ScriptLanguage
import lit.fass.server.security.Role.ADMIN
import lit.fass.server.security.Role.EXECUTOR
import lit.fass.server.security.SecurityManager
import org.apache.shiro.subject.Subject
import java.util.Collections.singletonList


/**
 * @author Michael Mair
 */
class ScriptRoutes(
    securityManager: SecurityManager,
    private val scriptEngines: List<ScriptEngine>
) : SecurityDirectives(securityManager) {

    companion object {
        private val log = this.logger()
    }

    val routes: Route = pathPrefix("script") {
        authenticate { subject ->
            path(segment().slash("test")) { language ->
                authorize(subject, listOf(ADMIN, EXECUTOR)) {
                    post {
                        entity(unmarshaller(Map::class.java)) { payload ->
                            testScript(language, payload, subject)
                        }
                    }
                }
            }
        }
    }

    private fun testScript(language: String, payload: Map<*, *>, subject: Subject): Route {
        val scriptLanguage: ScriptLanguage
        try {
            scriptLanguage = ScriptLanguage.valueOf(language.toUpperCase())
        } catch (ex: Exception) {
            return complete(
                BAD_REQUEST, mapOf(
                    "error" to "Language must be one of ${
                        ScriptLanguage.values().joinToString { it.name.toLowerCase() }
                    }"
                ), marshaller()
            )
        }
        log.info("Trying $language script for user ${subject.principal}")
        val scriptEngine = scriptEngines.find { it.isApplicable(scriptLanguage) }
            ?: return complete(
                BAD_REQUEST,
                mapOf("error" to "No script engine available for language $language"),
                marshaller()
            )

        val script = payload["script"] as String

        @Suppress("UNCHECKED_CAST")
        val data = payload["data"] as Map<String, Any?>
        return try {
            val result = scriptEngine.invoke(script, singletonList(data))
            complete(OK, result, marshaller())
        } catch (ex: Exception) {
            complete(BAD_REQUEST, mapOf("error" to ex.message), marshaller())
        }
    }
}