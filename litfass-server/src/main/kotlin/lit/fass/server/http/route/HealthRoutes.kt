package lit.fass.server.http.route

import akka.cluster.MemberStatus
import akka.cluster.typed.Cluster
import akka.http.javadsl.model.StatusCodes.OK
import akka.http.javadsl.model.StatusCodes.SERVICE_UNAVAILABLE
import akka.http.javadsl.server.AllDirectives
import akka.http.javadsl.server.Route
import lit.fass.server.logger

/**
 * @author Michael Mair
 */
class HealthRoutes(private val cluster: Cluster) : AllDirectives() {

    companion object {
        private val log = this.logger()
        private val readyStates = setOf(
            MemberStatus.up(),
            MemberStatus.weaklyUp()
        )
    }

    val routes: Route = concat(
        pathPrefix("ready") {
            get {
                val state = cluster.selfMember().status()
                log.debug("Current state is {}", state)
                if (readyStates.contains(state)) {
                    complete(OK)
                } else {
                    complete(SERVICE_UNAVAILABLE)
                }
            }
        },
        pathPrefix("health") {
            get {
                complete(OK)
            }
        }
    )
}