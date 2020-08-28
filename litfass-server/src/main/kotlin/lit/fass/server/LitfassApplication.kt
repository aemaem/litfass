package lit.fass.server

import akka.Done.done
import akka.actor.ActorSystem
import akka.actor.CoordinatedShutdown
import akka.actor.CoordinatedShutdown.PhaseActorSystemTerminate
import akka.actor.CoordinatedShutdown.PhaseBeforeServiceUnbind
import akka.http.javadsl.Http
import lit.fass.server.http.HealthRoutes
import org.slf4j.LoggerFactory
import java.util.concurrent.CompletableFuture.completedStage
import java.util.function.Supplier


/**
 * Main class.
 *
 * @author Michael Mair
 */
object LitfassApplication {

    private val log = LoggerFactory.getLogger("lit.fass.server.LitfassApplication")

    @JvmStatic
    fun main(args: Array<String>) {
        print("\nLITFASS\n\n")
        val system = ActorSystem.create("litfass")

        val http = Http.get(system)
        http.newServerAt("localhost", 8080)
            .bind(HealthRoutes().routes)
            .thenApply {
                val address = it.localAddress()
                log.info("Server online at http://${address.hostString}:${address.port}")
            }

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