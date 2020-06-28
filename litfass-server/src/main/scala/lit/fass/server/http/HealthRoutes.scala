package lit.fass.server.http

import akka.http.scaladsl.model.StatusCodes.OK
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route


object HealthRoutes {
  def apply(): HealthRoutes = new HealthRoutes()
}

/**
 * @author Michael Mair
 */
class HealthRoutes {

  val routes: Route =
    pathPrefix("health") {
      get {
        complete(OK)
      }
    }
}
