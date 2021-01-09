package lit.fass.server

import akka.Done.done
import akka.actor.CoordinatedShutdown
import akka.actor.CoordinatedShutdown.PhaseActorSystemTerminate
import akka.actor.CoordinatedShutdown.PhaseBeforeServiceUnbind
import akka.actor.typed.ActorSystem
import akka.actor.typed.SupervisorStrategy.restartWithBackoff
import akka.actor.typed.javadsl.Behaviors
import akka.actor.typed.javadsl.Behaviors.supervise
import akka.cluster.typed.Cluster
import akka.cluster.typed.ClusterSingleton
import akka.cluster.typed.SingletonActor
import akka.http.javadsl.server.directives.RouteDirectives
import akka.management.cluster.bootstrap.ClusterBootstrap
import akka.management.javadsl.AkkaManagement
import com.fasterxml.jackson.databind.SerializationFeature.WRITE_DATES_AS_TIMESTAMPS
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import lit.fass.server.actor.*
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
import java.time.Duration.ofSeconds
import java.util.concurrent.CompletableFuture.completedStage
import java.util.function.Supplier

/**
 * Main class.
 *
 * @author Michael Mair
 */
object LitfassApplication : RouteDirectives() {

    private val log = this.logger()

    @JvmStatic
    fun main(args: Array<String>) {
        print("\nLITFASS\n\n")

        System.setProperty("config.override_with_env_vars", "true")

        if (log.isDebugEnabled) {
            println("System properties:")
            val systemProperties = System.getProperties().entries.sortedBy { it.key.toString() }
            println(systemProperties.joinToString(separator = "\n") { "${it.key}=${it.value}" })
            println()
            println("Environment variables:")
            val environmentVariables = System.getenv().entries.sortedBy { it.key.toString() }
            println(environmentVariables.joinToString(separator = "\n") { "${it.key}=${it.value}" })
            println()
        }

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
            val configService = YamlConfigService(postgresPersistenceService, config)
            val securityManager = SecurityManager(config)

            val askScheduler = context.system.scheduler()
            val askTimout = config.getDuration("litfass.routes.ask-timeout")

            val schedulerService = QuartzCollectionSchedulerService(executionService, CollectionRetentionService(persistenceServices))
            val clusterSingleton = ClusterSingleton.get(context.system)
            val schedulerActor = clusterSingleton.init(
                SingletonActor.of(
                    supervise(SchedulerActor.create(schedulerService)).onFailure(restartWithBackoff(ofSeconds(1), ofSeconds(10), 0.2)),
                    "globalSchedulerActor"
                )
            )
            val configActor = context.spawn(ConfigActor.create(schedulerActor, configService, askTimout), "configActor")
            val collectionActor = context.spawn(CollectionActor.create(configService, executionService, persistenceServices), "collectionActor")
            val scriptActor = context.spawn(ScriptActor.create(scriptEngines), "scriptActor")

            val cluster = Cluster.get(context.system)

            HttpServer(
                concat(
                    HealthRoutes(cluster).routes,
                    CollectionRoutes(securityManager, collectionActor, askScheduler, askTimout).routes,
                    ConfigRoutes(securityManager, configActor, askScheduler, askTimout).routes,
                    ScriptRoutes(securityManager, scriptActor, askScheduler, askTimout).routes
                )
            ).startHttpServer(context.system)

            context.spawn(clusterEventLoggingBehavior(cluster), "clusterEventLoggingBehavior")
            AkkaManagement.get(context.system).start()
            ClusterBootstrap.get(context.system).start()

            configActor.tell(ConfigActor.InitializeConfigs())
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