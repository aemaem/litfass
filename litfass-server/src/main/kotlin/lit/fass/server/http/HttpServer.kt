package lit.fass.server.http

import akka.actor.typed.ActorSystem
import akka.http.javadsl.Http
import akka.http.javadsl.server.AllDirectives
import lit.fass.server.logger
import lit.fass.server.security.SecurityManager

/**
 * @author Michael Mair
 */
class HttpServer(private val securityManager: SecurityManager) : AllDirectives() {

    companion object {
        private val log = this.logger()
    }

    fun startHttpServer(system: ActorSystem<*>) {

        val http = Http.get(system)
        http.newServerAt("localhost", 8080)
            .bind(
                concat(
                    HealthRoutes().routes
                )
            )
            .handle { serverBinding, throwable ->
                if (throwable == null) {
                    val address = serverBinding.localAddress()
                    log.info("Server online at http://${address.hostString}:${address.port}")
                } else {
                    log.error("Failed to bind HTTP endpoint, terminating system", throwable)
                    system.terminate()
                }
            }
    }
}
