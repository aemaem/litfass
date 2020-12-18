package lit.fass.server.http

import akka.actor.typed.ActorSystem
import akka.http.javadsl.Http
import akka.http.javadsl.model.StatusCodes
import akka.http.javadsl.server.AllDirectives
import akka.http.javadsl.server.ExceptionHandler
import akka.http.javadsl.server.Route
import lit.fass.server.config.yaml.ConfigException
import lit.fass.server.execution.ExecutionException
import lit.fass.server.flow.FlowException
import lit.fass.server.logger
import lit.fass.server.persistence.PersistenceException
import lit.fass.server.retention.RetentionException
import lit.fass.server.schedule.SchedulerException

/**
 * @author Michael Mair
 */
class HttpServer(private val route: Route) : AllDirectives() {

    companion object {
        private val log = this.logger()
    }

    fun startHttpServer(system: ActorSystem<*>) {

        val exceptionHandler = ExceptionHandler.newBuilder()
            .match(ConfigException::class.java) { complete(StatusCodes.BAD_REQUEST, it.message) }
            .match(ExecutionException::class.java) { complete(StatusCodes.BAD_REQUEST, it.message) }
            .match(FlowException::class.java) { complete(StatusCodes.BAD_REQUEST, it.message) }
            .match(PersistenceException::class.java) { complete(StatusCodes.BAD_REQUEST, it.message) }
            .match(RetentionException::class.java) { complete(StatusCodes.BAD_REQUEST, it.message) }
            .match(SchedulerException::class.java) { complete(StatusCodes.BAD_REQUEST, it.message) }
            .match(Exception::class.java) { complete(StatusCodes.INTERNAL_SERVER_ERROR, it.message) }.build()

        val http = Http.get(system)
        http.newServerAt("0.0.0.0", 8080)
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
