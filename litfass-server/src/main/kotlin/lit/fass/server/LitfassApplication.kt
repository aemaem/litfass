package lit.fass.server

import akka.Done.done
import akka.actor.CoordinatedShutdown
import akka.actor.CoordinatedShutdown.PhaseActorSystemTerminate
import akka.actor.CoordinatedShutdown.PhaseBeforeServiceUnbind
import akka.actor.typed.ActorSystem
import akka.actor.typed.javadsl.Behaviors
import lit.fass.server.http.HttpServer
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
            val securityManager = SecurityManager(context.system.settings().config())

            HttpServer(securityManager).startHttpServer(context.system)

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