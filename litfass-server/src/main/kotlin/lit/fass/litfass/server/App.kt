@file:Suppress("EXPERIMENTAL_API_USAGE")

package lit.fass.litfass.server

/**
 * @author Michael Mair
 */
import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.SerializationFeature.INDENT_OUTPUT
import com.fasterxml.jackson.databind.SerializationFeature.WRITE_DATES_AS_TIMESTAMPS
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.ktor.application.Application
import io.ktor.application.call
import io.ktor.application.install
import io.ktor.application.log
import io.ktor.auth.*
import io.ktor.client.HttpClient
import io.ktor.client.engine.apache.Apache
import io.ktor.client.features.json.JacksonSerializer
import io.ktor.client.features.json.JsonFeature
import io.ktor.client.features.logging.LogLevel.ALL
import io.ktor.client.features.logging.LogLevel.NONE
import io.ktor.client.features.logging.Logging
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
import io.ktor.http.HttpStatusCode.Companion.InternalServerError
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
import io.ktor.util.getDigestFunction
import lit.fass.litfass.server.config.yaml.YamlConfigService
import lit.fass.litfass.server.execution.CollectionExecutionService
import lit.fass.litfass.server.flow.CollectionFlowService
import lit.fass.litfass.server.http.CollectionHttpService
import lit.fass.litfass.server.persistence.CollectionPersistenceService
import lit.fass.litfass.server.persistence.JdbcDataSource
import lit.fass.litfass.server.persistence.elasticsearch.ElasticsearchPersistenceService
import lit.fass.litfass.server.persistence.postgres.PostgresPersistenceService
import lit.fass.litfass.server.retention.CollectionRetentionService
import lit.fass.litfass.server.schedule.CollectionSchedulerService
import lit.fass.litfass.server.script.kts.KotlinScriptEngine
import org.apache.http.HttpHost
import org.elasticsearch.client.RestClient
import org.elasticsearch.client.RestHighLevelClient
import org.slf4j.event.Level.DEBUG
import org.slf4j.event.Level.INFO
import java.io.File
import java.net.URI
import java.time.OffsetDateTime
import java.time.ZoneOffset.UTC
import java.util.*


fun main(args: Array<String>): Unit = io.ktor.server.netty.EngineMain.main(args)

fun Application.litfassModule() {
    val testing = environment.config.propertyOrNull("testing")?.getString() == true.toString()
    if (testing) {
        log.warn("Application in test mode")
    }

    val digestFunction = getDigestFunction("SHA-256", salt = "lit.fass")
    var users = environment.config.configList("litfass.config.security.users")
        .associateBy({ it.property("name").getString() }, {
            digestFunction.invoke(it.property("password").getString())
        })
    if (!users.containsKey("admin")) {
        log.warn("No user defined with name admin. Generating admin user")
        val adminPassword: String
        if (testing) {
            adminPassword = "admin"
        } else {
            adminPassword = UUID.randomUUID().toString()
            log.warn("Generated admin password: $adminPassword")
        }
        val adminUser = mapOf("admin" to digestFunction.invoke(adminPassword))
        users = adminUser + users
    }
    log.info("Registered ${users.size} users")
    val userTable = UserHashedTableAuth(digestFunction, users)
    install(Authentication) {
        basic {
            realm = "LITFASS"
            validate { userTable.authenticate(it) }
        }
    }
    install(ContentNegotiation) {
        jackson {
            enable(INDENT_OUTPUT)
        }
    }
    install(CallLogging) {
        level = if (testing) DEBUG else INFO
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
                CSS -> CachingOptions(
                    CacheControl.MaxAge(maxAgeSeconds = 24 * 60 * 60),
                    expires = null as? GMTDate?
                )
                else -> null
            }
        }
    }
    install(DataConversion)

    log.info("Instantiating JSON mapper")
    val jsonMapper = jacksonObjectMapper().apply {
        registerModule(JavaTimeModule())
        configure(WRITE_DATES_AS_TIMESTAMPS, false)
    }
    val persistenceServices = mutableListOf<CollectionPersistenceService>()
    log.info("Instantiating postgresql database connection")
    val postgresqlUrl = environment.config.property("litfass.postgresql.jdbc.url").getString()
    val postgresqlDatabase = environment.config.property("litfass.postgresql.jdbc.database").getString()
    val postgresqlUser = environment.config.property("litfass.postgresql.jdbc.user").getString()
    val postgresqlPassword = environment.config.property("litfass.postgresql.jdbc.password").getString()
    val postgresqlDatasource = JdbcDataSource(postgresqlUrl, postgresqlDatabase, postgresqlUser, postgresqlPassword)
    log.info("Instantiating postgresql persistence service")
    val postgresqlPersistenceService = PostgresPersistenceService(postgresqlDatasource, jsonMapper)
    persistenceServices.add(postgresqlPersistenceService)
    if (environment.config.property("litfass.elasticsearch.enabled").getString() == true.toString()) {
        log.info("Instantiating elasticsearch client")
        val elasticsearch = RestHighLevelClient(RestClient.builder(*environment.config
            .property("litfass.elasticsearch.client.urls")
            .getString()
            .split(",")
            .map { URI(it) }
            .map { HttpHost(it.host, it.port, it.scheme) }.toTypedArray()
        )
        )
        log.info("Instantiating elasticsearch persistence service")
        val elasticsearchPersistenceService = ElasticsearchPersistenceService(elasticsearch, jsonMapper)
        persistenceServices.add(elasticsearchPersistenceService)
    }
    log.info("Instantiating http service")
    val httpService = CollectionHttpService(HttpClient(Apache) {
        install(JsonFeature) {
            serializer = JacksonSerializer()
        }
        install(Logging) {
            level = if (testing) ALL else NONE
        }
        engine {
            followRedirects = true
        }
    })
    log.info("Instantiating script engines")
    val scriptEngines = listOf(KotlinScriptEngine())
    log.info("Instantiating flow service")
    val flowService = CollectionFlowService(httpService, scriptEngines)
    log.info("Instantiating execution service")
    val executionService = CollectionExecutionService(flowService, persistenceServices)
    log.info("Instantiating retention service")
    val retentionService = CollectionRetentionService(
        environment.config.property("litfass.config.retention.scheduled").getString(),
        persistenceServices
    )
    log.info("Instantiating scheduler service")
    val schedulerService = CollectionSchedulerService(executionService, retentionService)
    log.info("Instantiating config service")
    val configService = YamlConfigService(postgresqlPersistenceService, schedulerService)
    val configCollectionPath = environment.config.propertyOrNull("litfass.config.collection.path")
    if (configCollectionPath != null) {
        configService.readRecursively(File(configCollectionPath.getString()))
    }

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

            val collectionHeaders = call.request.headers.entries()
                .associateBy({ entry -> entry.key }, { entry -> entry.value.joinToString(",") })
            log.debug("Got headers $collectionHeaders for collection $collection")
            val collectionMetaData = call.request.queryParameters.entries()
                .associateBy({ entry -> entry.key }, { entry -> entry.value.joinToString(",") })
            log.debug("Got collection $collection and meta data $collectionMetaData")
            val collectionData: Map<String, Any?> =
                jsonMapper.readValue(call.receiveStream(), object : TypeReference<Map<String, Any?>>() {})
            log.debug("Payload received: $collectionData")

            val data = mutableMapOf<String, Any?>("timestamp" to OffsetDateTime.now(UTC))
            data.putAll(collectionHeaders)
            data.putAll(collectionMetaData)
            data.putAll(collectionData)

            try {
                executionService.execute(configService.getConfig(collection), data)
            } catch (ex: Exception) {
                log.error("Exception during execution of collection $collection", ex)
                call.respond(InternalServerError, "{\"error\":\"${ex.message}\"}")
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
                // todo: implement pagination
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
                    log.error("Unable to read config", ex)
                    call.respond(BadRequest, "{\"error\":\"Unable to read config: ${ex.message}\"}")
                    return@post
                }
                call.respond(OK)
            }
            post("/script/{extension}/test") {
                val principal = call.principal<UserIdPrincipal>()!!
                val extension = call.parameters["extension"]
                log.info("Trying $extension script for user ${principal.name}")
                if (extension.isNullOrBlank()) {
                    call.respond(BadRequest, "{\"error\":\"Extension must not be blank\"}")
                    return@post
                }
                val scriptEngine = scriptEngines.find { it.isApplicable(extension) }
                if (scriptEngine == null) {
                    call.respond(BadRequest, "{\"error\":\"No script engine available for extension $extension\"}")
                    return@post
                }

                val body: Map<String, Any?> =
                    jsonMapper.readValue(call.receiveStream(), object : TypeReference<Map<String, Any?>>() {})
                val script = body["script"] as String
                @Suppress("UNCHECKED_CAST")
                val data = body["data"] as Map<String, Any?>
                try {
                    val result = scriptEngine.invoke(script, data)
                    call.respond(result)
                } catch (ex: Exception) {
                    call.respond(BadRequest, "{\"error\":\"${ex.message}\"}")
                    return@post
                }
            }
        }
    }
}

