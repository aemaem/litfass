package lit.fass.server.http

import akka.http.scaladsl.model.StatusCodes.{Forbidden, Unauthorized}
import akka.http.scaladsl.model.headers.HttpCredentials
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import lit.fass.server.security.Role.{ADMIN, READER}
import lit.fass.server.security.SecurityManager
import org.apache.shiro.subject.Subject

object CollectionRoutes {
  def apply(securityManager: SecurityManager): CollectionRoutes = new CollectionRoutes(securityManager)
}

/**
 * @author Michael Mair
 */
class CollectionRoutes(securityManager: SecurityManager) extends SecurityRoutes(securityManager) {

  val routes: Route =
    extractCredentials { creds: Option[HttpCredentials] =>
      pathPrefix("collections") {
        path(Segment) { collection =>
          concat(
            post {
              ??? //todo: implement
            },
            get {
              ??? //todo: implement
            },
            path(Segment) { id =>
              val subject: Subject = login(creds)
              if (!subject.isAuthenticated) {
                complete(Unauthorized)
              }
              if (!subject.hasRole(ADMIN.toString) && !subject.hasRole(READER.toString)) {
                complete(Forbidden, s"Roles ${ADMIN} or ${READER} required")
              }
              ??? //todo: implement
            }
          )
        }
      }
    }
}
