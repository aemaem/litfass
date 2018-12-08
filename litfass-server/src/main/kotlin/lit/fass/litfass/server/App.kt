@file:Suppress("EXPERIMENTAL_API_USAGE")

package lit.fass.litfass.server

/**
 * @author Michael Mair
 */
import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.SerializationFeature.INDENT_OUTPUT
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.ktor.application.Application
import io.ktor.application.call
import io.ktor.application.install
import io.ktor.application.log
import io.ktor.auth.*
import io.ktor.client.HttpClient
import io.ktor.client.engine.apache.Apache
import io.ktor.features.*
import io.ktor.http.CacheControl
import io.ktor.http.ContentType.Text.CSS
import io.ktor.http.HttpHeaders.Authorization
import io.ktor.http.HttpMethod.Companion.Delete
import io.ktor.http.HttpMethod.Companion.Options
import io.ktor.http.HttpMethod.Companion.Patch
import io.ktor.http.HttpMethod.Companion.Post
import io.ktor.http.HttpMethod.Companion.Put
import io.ktor.http.HttpStatusCode.Companion.BadRequest
import io.ktor.http.HttpStatusCode.Companion.NoContent
import io.ktor.http.HttpStatusCode.Companion.OK
import io.ktor.http.content.CachingOptions
import io.ktor.jackson.jackson
import io.ktor.request.path
import io.ktor.request.receiveStream
import io.ktor.response.respond
import io.ktor.routing.delete
import io.ktor.routing.get
import io.ktor.routing.post
import io.ktor.routing.routing
import io.ktor.util.date.GMTDate
import io.ktor.util.toMap
import lit.fass.litfass.server.config.yaml.YamlConfigService
import lit.fass.litfass.server.flow.CollectionFlowService
import lit.fass.litfass.server.http.CollectionHttpService
import lit.fass.litfass.server.persistence.PersistenceClient.Companion.ID_KEY
import lit.fass.litfass.server.persistence.elasticsearch.EsPersistenceClient
import lit.fass.litfass.server.script.kts.KotlinScriptEngine
import org.apache.http.HttpHost
import org.elasticsearch.client.RestClient
import org.elasticsearch.client.RestHighLevelClient
import org.slf4j.event.Level.INFO
import java.io.File
import java.net.URI
import java.time.OffsetDateTime
import java.time.ZoneOffset.UTC


fun main(args: Array<String>): Unit = io.ktor.server.netty.EngineMain.main(args)

@JvmOverloads
fun Application.module(testing: Boolean = false) {
    if (testing) {
        log.warn("Application in test mode")
    }

    install(Authentication) {
        basic {
            realm = "LITFASS"
            validate { if (it.name == "admin" && it.password == "admin") UserIdPrincipal(it.name) else null }
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
        method(Post)
        method(Put)
        method(Delete)
        method(Options)
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

    val jsonMapper = jacksonObjectMapper()
    val elasticsearch = RestHighLevelClient(RestClient.builder(*environment.config
        .property("litfass.elasticsearch.client.urls")
        .getString()
        .split(",")
        .map { URI(it) }
        .map { HttpHost(it.host, it.port, it.scheme) }.toTypedArray()
    )
    )
    val persistenceClient = EsPersistenceClient(elasticsearch, jsonMapper)

    val configService = YamlConfigService()
    val configCollectionPath = environment.config.propertyOrNull("litfass.config.collection.path")
    if (configCollectionPath != null) {
        configService.readRecursively(File(configCollectionPath.getString()))
    }
    val httpService = CollectionHttpService(HttpClient(Apache))
    val scriptEngines = listOf(KotlinScriptEngine())
    val flowService = CollectionFlowService(httpService, scriptEngines)

    routing {
        get("/health") {
            call.respond(mapOf("status" to "OK"))
        }
        post("/collections/{collection}") {
            val collection = call.parameters["collection"]
            if (collection.isNullOrBlank()) {
                call.respond(BadRequest, "{\"error\":\"Collection must not be blank\"}")
                return@post
            }

            val collectionMetaData = call.request.queryParameters.toMap()
            log.debug("Got collection $collection and meta data $collectionMetaData")
            val collectionData: Map<String, Any?> =
                jsonMapper.readValue(call.receiveStream(), object : TypeReference<Map<String, Any?>>() {})
            log.debug("Payload received: $collectionData")

            val data = mutableMapOf<String, Any?>("timestamp" to OffsetDateTime.now(UTC))
            data.putAll(collectionMetaData)
            data.putAll(collectionData)

            val config = configService.getConfig(collection)
            val dataToPersist = flowService.execute(data, config)
            if (dataToPersist.containsKey(ID_KEY)) {
                persistenceClient.save(collection, dataToPersist[ID_KEY], dataToPersist)
            } else {
                persistenceClient.save(collection, dataToPersist)
            }
            call.respond(OK)
        }

        authenticate {
            get("/") {
                call.respond(
                    mapOf(
                        "application" to "LITFASS",
                        "description" to "Lightweight Integrated Tailorable Flow Aware Software Service"
                    )
                )
            }
            get("/configs") {
                val principal = call.principal<UserIdPrincipal>()!!
                log.debug("Getting all configs for user ${principal.name}")
                call.respond(jsonMapper.writeValueAsString(configService.getConfigs()))
            }
            get("/configs/{collection}") {
                val collection = call.parameters["collection"]
                if (collection.isNullOrBlank()) {
                    call.respond(BadRequest, "{\"error\":\"Collection must not be blank\"}")
                    return@get
                }
                val principal = call.principal<UserIdPrincipal>()!!
                log.debug("Getting config $collection for user ${principal.name}")
                call.respond(jsonMapper.writeValueAsString(configService.getConfig(collection)))
            }
            delete("/configs/{collection}") {
                val collection = call.parameters["collection"]
                if (collection.isNullOrBlank()) {
                    call.respond(BadRequest, "{\"error\":\"Collection must not be blank\"}")
                    return@delete
                }
                val principal = call.principal<UserIdPrincipal>()!!
                log.debug("Removing config $collection for user ${principal.name}")
                configService.removeConfig(collection)
                call.respond(NoContent)
            }
            post("/configs") {
                val principal = call.principal<UserIdPrincipal>()!!
                log.info("Adding config for user ${principal.name}")
                try {
                    configService.readConfig(call.receiveStream())
                } catch (ex: Exception) {
                    call.respond(BadRequest, "{\"error\":\"Unable to read config: ${ex.message}\"}")
                    return@post
                }
                call.respond(OK)
            }
            post("/script/{language}/test") {
                val principal = call.principal<UserIdPrincipal>()!!
                val language = call.parameters["language"]
                log.info("Trying $language config for user ${principal.name}")
                if (language.isNullOrBlank()) {
                    call.respond(BadRequest, "{\"error\":\"Language must not be blank\"}")
                    return@post
                }
                val scriptEngine = scriptEngines.find { it.isApplicable(language) }
                if (scriptEngine == null) {
                    call.respond(BadRequest, "{\"error\":\"No script engine available for language $language\"}")
                    return@post
                }

                val body: Map<String, Any?> =
                    jsonMapper.readValue(call.receiveStream(), object : TypeReference<Map<String, Any?>>() {})
                val script = body["script"] as String
                @Suppress("UNCHECKED_CAST")
                val data = body["data"] as Map<String, Any?>
                val result = scriptEngine.invoke(script, data)
                call.respond(result)
            }
        }
    }
}

