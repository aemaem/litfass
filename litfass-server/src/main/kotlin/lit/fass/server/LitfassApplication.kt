package lit.fass.server

import akka.Done.done
import akka.actor.CoordinatedShutdown
import akka.actor.CoordinatedShutdown.PhaseActorSystemTerminate
import akka.actor.CoordinatedShutdown.PhaseBeforeServiceUnbind
import akka.actor.typed.ActorSystem
import akka.actor.typed.javadsl.Behaviors
import com.fasterxml.jackson.databind.SerializationFeature.WRITE_DATES_AS_TIMESTAMPS
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import lit.fass.server.config.yaml.YamlConfigService
import lit.fass.server.execution.CollectionExecutionService
import lit.fass.server.flow.CollectionFlowService
import lit.fass.server.http.CollectionHttpService
import lit.fass.server.http.HttpServer
import lit.fass.server.http.route.CollectionRoutes
import lit.fass.server.http.route.ConfigRoutes
import lit.fass.server.http.route.HealthRoutes
import lit.fass.server.http.route.ScriptRoutes
import lit.fass.server.persistence.JdbcDataSource
import lit.fass.server.persistence.postgres.PostgresPersistenceService
import lit.fass.server.retention.CollectionRetentionService
import lit.fass.server.schedule.QuartzCollectionSchedulerService
import lit.fass.server.script.groovy.GroovyScriptEngine
import lit.fass.server.security.SecurityManager
import java.util.concurrent.CompletableFuture.completedStage
import java.util.function.Supplier

/**
 * Main class.
 *
 * @author Michael Mair
 */
object LitfassApplication {

    private val log = this.logger()

    @JvmStatic
    fun main(args: Array<String>) {
        print("\nLITFASS\n\n")

        val rootBehavior = Behaviors.setup<Void> { context ->
            val config = context.system.settings().config()

            val jsonMapper = jacksonObjectMapper().apply {
                registerModule(JavaTimeModule())
                configure(WRITE_DATES_AS_TIMESTAMPS, false)
            }
            val jdbcDataSource = JdbcDataSource(
                config.getString("litfass.jdbc.url"),
                config.getString("litfass.jdbc.database"),
                config.getString("litfass.jdbc.username"),
                config.getString("litfass.jdbc.password"),
                config.getInt("litfass.jdbc.poolSize")
            )

            val postgresPersistenceService = PostgresPersistenceService(jdbcDataSource, jsonMapper)
            val persistenceServices = listOf(postgresPersistenceService)

            val groovyScriptEngine = GroovyScriptEngine()
            val scriptEngines = listOf(groovyScriptEngine)

            val executionService = CollectionExecutionService(CollectionFlowService(CollectionHttpService(jsonMapper), scriptEngines), persistenceServices)
            val schedulerService = QuartzCollectionSchedulerService(executionService, CollectionRetentionService(persistenceServices))
            val configService = YamlConfigService(postgresPersistenceService, schedulerService, config)
            configService.initializeConfigs()
            val securityManager = SecurityManager(config)

            val route = HealthRoutes().routes
                .orElse(CollectionRoutes(securityManager, configService, executionService, persistenceServices).routes)
                .orElse(ConfigRoutes(securityManager, configService).routes)
                .orElse(ScriptRoutes(securityManager, scriptEngines).routes)
            HttpServer(route).startHttpServer(context.system)

            Behaviors.empty()
        }
        val system: ActorSystem<Void> = ActorSystem.create(rootBehavior, "litfass")

        CoordinatedShutdown.get(system).addTask(PhaseBeforeServiceUnbind(), "shutdownLogging", Supplier {
            log.info("Application is being terminated...")
            completedStage(done())
        })
        CoordinatedShutdown.get(system).addTask(PhaseActorSystemTerminate(), "offLogging", Supplier {
            log.info("Application terminated. Servus!")
            completedStage(done())
        })
    }
}