package lit.fass.litfass.server

import com.fasterxml.jackson.databind.SerializationFeature.INDENT_OUTPUT
import io.ktor.application.Application
import io.ktor.application.call
import io.ktor.application.install
import io.ktor.auth.*
import io.ktor.features.*
import io.ktor.http.CacheControl
import io.ktor.http.ContentType.Text.CSS
import io.ktor.http.ContentType.Text.Plain
import io.ktor.http.HttpHeaders.Authorization
import io.ktor.http.HttpMethod.Companion.Delete
import io.ktor.http.HttpMethod.Companion.Options
import io.ktor.http.HttpMethod.Companion.Patch
import io.ktor.http.HttpMethod.Companion.Put
import io.ktor.http.cio.websocket.Frame
import io.ktor.http.cio.websocket.readText
import io.ktor.http.content.CachingOptions
import io.ktor.jackson.jackson
import io.ktor.request.path
import io.ktor.response.respond
import io.ktor.response.respondText
import io.ktor.routing.get
import io.ktor.routing.routing
import io.ktor.util.date.GMTDate
import io.ktor.websocket.webSocket
import org.slf4j.event.Level.INFO
import java.time.Duration

fun main(args: Array<String>): Unit = io.ktor.server.netty.EngineMain.main(args)

@JvmOverloads
fun Application.module(testing: Boolean = false) {
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

    install(DefaultHeaders) {
        header("X-Engine", "Ktor") // will send this header with each response
    }

    install(io.ktor.websocket.WebSockets) {
        pingPeriod = Duration.ofSeconds(15)
        timeout = Duration.ofSeconds(15)
        maxFrameSize = Long.MAX_VALUE
        masking = false
    }

    routing {
        get("/") {
            call.respondText("HELLO WORLD!", contentType = Plain)
        }

        authenticate {
            get("/secure/basic") {
                val principal = call.principal<UserIdPrincipal>()!!
                call.respondText("Hello ${principal.name}")
            }
        }

        get("/json/jackson") {
            call.respond(mapOf("hello" to "world"))
        }

        webSocket("/websocket/echo") {
            send(Frame.Text("Hi from server"))
            while (true) {
                val frame = incoming.receive()
                if (frame is Frame.Text) {
                    send(Frame.Text("Client said: " + frame.readText()))
                }
            }
        }
    }
}

