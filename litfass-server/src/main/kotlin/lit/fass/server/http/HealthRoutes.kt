package lit.fass.server.http

import akka.http.javadsl.model.StatusCodes.OK
import akka.http.javadsl.server.AllDirectives
import akka.http.javadsl.server.Route


/**
 * @author Michael Mair
 */
class HealthRoutes : AllDirectives() {

    val routes: Route =
        pathPrefix("health") {
            get {
                complete(OK)
            }
        }
}