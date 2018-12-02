package lit.fass.litfass.server

/**
 * @author Michael Mair
 */
import com.fasterxml.jackson.databind.SerializationFeature.INDENT_OUTPUT
import io.ktor.application.Application
import io.ktor.application.call
import io.ktor.application.install
import io.ktor.application.log
import io.ktor.auth.*
import io.ktor.features.*
import io.ktor.http.CacheControl
import io.ktor.http.ContentType.Text.CSS
import io.ktor.http.HttpHeaders.Authorization
import io.ktor.http.HttpMethod.Companion.Delete
import io.ktor.http.HttpMethod.Companion.Options
import io.ktor.http.HttpMethod.Companion.Patch
import io.ktor.http.HttpMethod.Companion.Put
import io.ktor.http.HttpStatusCode.Companion.OK
import io.ktor.http.content.CachingOptions
import io.ktor.jackson.jackson
import io.ktor.request.path
import io.ktor.request.receiveText
import io.ktor.response.respond
import io.ktor.response.respondText
import io.ktor.routing.get
import io.ktor.routing.post
import io.ktor.routing.routing
import io.ktor.util.date.GMTDate
import io.ktor.util.toMap
import lit.fass.litfass.server.persistence.elasticsearch.EsPersistenceClient
import org.slf4j.event.Level.INFO
import java.net.URI


fun main(args: Array<String>): Unit = io.ktor.server.netty.EngineMain.main(args)

@JvmOverloads
fun Application.module(testing: Boolean = false) {
    if (testing) {
        log.warn("Application in test mode")
    }

    install(Authentication) {
        basic {
            realm = "LITFASS"
            validate { if (it.name == "test" && it.password == "password") UserIdPrincipal(it.name) else null }
        }
    }
    install(ContentNegotiation) {
        jackson {
            enable(INDENT_OUTPUT)
        }
    }
    install(CallLogging) {
        level = INFO
        filter { call -> call.request.path().startsWith("/") }
    }
    install(ConditionalHeaders)
    install(CORS) {
        method(Options)
        method(Put)
        method(Delete)
        method(Patch)
        header(Authorization)
        allowCredentials = true
        anyHost()
    }
    install(CachingHeaders) {
        options { outgoingContent ->
            when (outgoingContent.contentType?.withoutParameters()) {
                CSS -> CachingOptions(CacheControl.MaxAge(maxAgeSeconds = 24 * 60 * 60), expires = null as? GMTDate?)
                else -> null
            }
        }
    }
    install(DataConversion)

    //todo: read persistence URI from app config
    val persistenceClient = EsPersistenceClient(URI("http://localhost:9200"))

    routing {
        get("/") {
            call.respond(
                mapOf(
                    "application" to "LITFASS",
                    "description" to "Lightweight Integrated Tailorable Flow Aware Software Service"
                )
            )
        }

        post("/collections/{collection}") {
            val collection = call.parameters["collection"]
            val collectionMetaData = call.request.queryParameters.toMap()
            log.debug("Got collection $collection and meta data $collectionMetaData")

            val collectionData = call.receiveText()
            log.debug("Payload received: $collectionData")

            persistenceClient.save(collection, collectionData, collectionMetaData)
            call.respond(OK, "")
        }

        authenticate {
            get("/secure/basic") {
                val principal = call.principal<UserIdPrincipal>()!!
                call.respondText("Hello ${principal.name}")
            }
        }
    }
}

