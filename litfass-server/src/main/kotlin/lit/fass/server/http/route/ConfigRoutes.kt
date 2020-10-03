package lit.fass.server.http.route

import akka.http.javadsl.marshallers.jackson.Jackson.marshaller
import akka.http.javadsl.model.StatusCodes.*
import akka.http.javadsl.server.PathMatchers.segment
import akka.http.javadsl.server.Route
import akka.util.ByteString.emptyByteString
import lit.fass.server.config.ConfigService
import lit.fass.server.http.SecurityDirectives
import lit.fass.server.logger
import lit.fass.server.security.Role.*
import lit.fass.server.security.SecurityManager
import org.apache.shiro.subject.Subject
import java.io.ByteArrayInputStream


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
        authenticate { subject ->
            concat(
                path(segment()) { collection ->
                    concat(
                        get {
                            authorize(subject, listOf(ADMIN, READER)) {
                                getConfig(collection, subject)
                            }
                        },
                        delete {
                            authorize(subject, listOf(ADMIN, WRITER)) {
                                deleteConfig(collection, subject)
                            }
                        }
                    )
                },
                post {
                    authorize(subject, listOf(ADMIN, WRITER)) {
                        extractMaterializer { materializer ->
                            extractDataBytes { data ->
                                onSuccess(data.runFold(emptyByteString(), { acc, i -> acc.concat(i) }, materializer)) { rawData ->
                                    addConfig(rawData.toArray())
                                }
                            }
                        }
                    }
                },
                get {
                    authorize(subject, listOf(ADMIN, READER)) {
                        getConfigs(subject)
                    }
                }
            )
        }
    }

    private fun getConfigs(subject: Subject): Route {
        return try {
            log.debug("Getting all configs for user ${subject.principal}")
            val result = configService.getConfigs()
            complete(OK, result, marshaller())
        } catch (ex: Exception) {
            complete(BAD_REQUEST, mapOf("error" to ex.message), marshaller())
        }
    }

    private fun getConfig(collection: String, subject: Subject): Route {
        if (collection.isBlank()) {
            return complete(BAD_REQUEST, mapOf("error" to "Collection must not be blank"), marshaller())
        }

        return try {
            log.debug("Getting config $collection for user ${subject.principal}")
            val result = configService.getConfig(collection)
            complete(OK, result, marshaller())
        } catch (ex: Exception) {
            complete(BAD_REQUEST, mapOf("error" to ex.message), marshaller())
        }
    }

    private fun deleteConfig(collection: String, subject: Subject): Route {
        if (collection.isBlank()) {
            return complete(BAD_REQUEST, mapOf("error" to "Collection must not be blank"), marshaller())
        }
        log.debug("Removing config $collection for user ${subject.principal}")
        configService.removeConfig(collection)
        return complete(NO_CONTENT)
    }

    private fun addConfig(payload: ByteArray): Route {
        try {
            configService.readConfig(ByteArrayInputStream(payload))
        } catch (ex: Exception) {
            log.error("Unable to read config", ex)
            complete(BAD_REQUEST, mapOf("error" to "Unable to read config: ${ex.message}"), marshaller())
        }
        return complete(NO_CONTENT)
    }

}