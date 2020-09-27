package lit.fass.server.http.route

import akka.http.javadsl.model.StatusCodes
import akka.http.javadsl.server.AllDirectives
import akka.http.javadsl.server.Route

/**
 * @author Michael Mair
 */
class HealthRoutes : AllDirectives() {

    val routes: Route = pathPrefix("health") {
        get {
            complete(StatusCodes.OK)
        }
    }
}