package lit.fass.server.http.route

import akka.actor.typed.ActorRef
import akka.actor.typed.Scheduler
import akka.actor.typed.javadsl.AskPattern.ask
import akka.http.javadsl.marshallers.jackson.Jackson.marshaller
import akka.http.javadsl.marshallers.jackson.Jackson.unmarshaller
import akka.http.javadsl.model.StatusCodes.OK
import akka.http.javadsl.server.PathMatchers.segment
import akka.http.javadsl.server.Route
import akka.japi.function.Function
import lit.fass.server.actor.ScriptActor.*
import lit.fass.server.http.SecurityDirectives
import lit.fass.server.logger
import lit.fass.server.script.ScriptLanguage
import lit.fass.server.security.Role.ADMIN
import lit.fass.server.security.Role.EXECUTOR
import lit.fass.server.security.SecurityManager
import org.apache.shiro.subject.Subject
import java.time.Duration
import java.util.concurrent.CompletionStage

/**
 * @author Michael Mair
 */
class ScriptRoutes(
    securityManager: SecurityManager,
    private val scriptActor: ActorRef<Message>,
    private val scheduler: Scheduler,
    private val timeout: Duration
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
                            onSuccess(testScript(language, payload, subject)) { complete(OK, it.result, marshaller()) }
                        }
                    }
                }
            }
        }
    }

    private fun testScript(language: String, payload: Map<*, *>, subject: Subject): CompletionStage<ScriptResult> {
        val scriptLanguage = ScriptLanguage.valueOf(language.toUpperCase())
        log.info("Trying $language script for user ${subject.principal}")
        return ask(scriptActor, Function<ActorRef<ScriptResult>, Message?> { ref ->
            TestScript(scriptLanguage, payload, ref)
        }, timeout, scheduler)

    }
}