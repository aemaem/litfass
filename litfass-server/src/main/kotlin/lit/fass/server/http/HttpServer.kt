package lit.fass.server.http

import akka.actor.typed.ActorSystem
import akka.http.javadsl.Http
import akka.http.javadsl.server.AllDirectives
import akka.http.javadsl.server.Route
import lit.fass.server.logger

/**
 * @author Michael Mair
 */
class HttpServer(private val route: Route) : AllDirectives() {

    companion object {
        private val log = this.logger()
    }

    fun startHttpServer(system: ActorSystem<*>) {

        val http = Http.get(system)
        http.newServerAt("localhost", 8080)
            .bind(route)
            //todo: exception handling: https://doc.akka.io/docs/akka-http/current/routing-dsl/exception-handling.html
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
