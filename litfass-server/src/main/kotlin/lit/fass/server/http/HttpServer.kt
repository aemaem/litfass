package lit.fass.server.http

import akka.actor.typed.ActorSystem
import akka.http.javadsl.Http
import akka.http.javadsl.model.StatusCodes
import akka.http.javadsl.server.AllDirectives
import akka.http.javadsl.server.ExceptionHandler
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

        val exceptionHandler = ExceptionHandler.newBuilder()
            .match(Exception::class.java) {
                //todo: exception handling: https://doc.akka.io/docs/akka-http/current/routing-dsl/exception-handling.html
                log.error(it.message, it)
                complete(StatusCodes.IM_A_TEAPOT)
            }.build()

        val http = Http.get(system)
        http.newServerAt("localhost", 8080)
            .bind(handleExceptions(exceptionHandler) {
                route
            })
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
