package lit.fass.server.http.route

import akka.actor.typed.ActorRef
import akka.actor.typed.Scheduler
import akka.actor.typed.javadsl.AskPattern.ask
import akka.http.javadsl.marshallers.jackson.Jackson.marshaller
import akka.http.javadsl.model.StatusCodes.NO_CONTENT
import akka.http.javadsl.model.StatusCodes.OK
import akka.http.javadsl.server.PathMatchers.segment
import akka.http.javadsl.server.Route
import akka.japi.function.Function
import akka.util.ByteString.emptyByteString
import lit.fass.server.actor.ConfigActor.*
import lit.fass.server.config.yaml.ConfigException
import lit.fass.server.http.SecurityDirectives
import lit.fass.server.logger
import lit.fass.server.security.Role.*
import lit.fass.server.security.SecurityManager
import org.apache.shiro.subject.Subject
import java.io.ByteArrayInputStream
import java.time.Duration
import java.util.concurrent.CompletionStage


/**
 * @author Michael Mair
 */
class ConfigRoutes(
    securityManager: SecurityManager,
    private val configActor: ActorRef<Message>,
    private val scheduler: Scheduler,
    private val timeout: Duration
) : SecurityDirectives(securityManager) {

    companion object {
        private val log = this.logger()
    }

    val routes: Route = pathPrefix("configs") {
        authenticate { subject ->
            concat(
                path(segment()) { collection ->
                    concat(
                        get {
                            authorize(subject, listOf(ADMIN, READER)) {
                                onSuccess(getConfig(collection, subject)) { complete(OK, it.response, marshaller()) }
                            }
                        },
                        delete {
                            authorize(subject, listOf(ADMIN, WRITER)) {
                                onSuccess(deleteConfig(collection, subject)) { complete(NO_CONTENT) }
                            }
                        }
                    )
                },
                post {
                    authorize(subject, listOf(ADMIN, WRITER)) {
                        extractMaterializer { materializer ->
                            extractDataBytes { data ->
                                onSuccess(data.runFold(emptyByteString(), { acc, i -> acc.concat(i) }, materializer)) { rawData ->
                                    onSuccess(addConfig(rawData.toArray(), subject)) { complete(NO_CONTENT) }
                                }
                            }
                        }
                    }
                },
                get {
                    authorize(subject, listOf(ADMIN, READER)) {
                        onSuccess(getConfigs(subject)) { complete(OK, it.response, marshaller()) }
                    }
                }
            )
        }
    }

    private fun getConfigs(subject: Subject): CompletionStage<Configs> {
        log.debug("Getting all configs for user ${subject.principal}")
        return ask(configActor, Function<ActorRef<Configs>, Message?> { ref ->
            GetConfigs(ref)
        }, timeout, scheduler)
    }

    private fun getConfig(collection: String, subject: Subject): CompletionStage<Config> {
        log.debug("Getting config $collection for user ${subject.principal}")
        if (collection.isBlank()) {
            throw ConfigException("Collection must not be blank")
        }
        return ask(configActor, Function<ActorRef<Config>, Message?> { ref ->
            GetConfig(collection, ref)
        }, timeout, scheduler)
    }

    private fun deleteConfig(collection: String, subject: Subject): CompletionStage<Done> {
        log.debug("Removing config $collection for user ${subject.principal}")
        if (collection.isBlank()) {
            throw ConfigException("Collection must not be blank")
        }
        return ask(configActor, Function<ActorRef<Done>, Message?> { ref ->
            RemoveConfig(collection, ref)
        }, timeout, scheduler)
    }

    private fun addConfig(payload: ByteArray, subject: Subject): CompletionStage<Done> {
        log.debug("Adding config for user ${subject.principal}")
        return ask(configActor, Function<ActorRef<Done>, Message?> { ref ->
            AddConfig(ByteArrayInputStream(payload), ref)
        }, timeout, scheduler)
    }

}