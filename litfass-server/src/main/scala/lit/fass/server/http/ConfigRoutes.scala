package lit.fass.server.http

import akka.http.scaladsl.model.StatusCodes.{Forbidden, Unauthorized}
import akka.http.scaladsl.model.headers.HttpCredentials
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import lit.fass.server.security.Role.{ADMIN, READER, WRITER}
import lit.fass.server.security.SecurityManager
import org.apache.shiro.subject.Subject


object ConfigRoutes {
  def apply(securityManager: SecurityManager): ConfigRoutes = new ConfigRoutes(securityManager)
}

/**
 * @author Michael Mair
 */
class ConfigRoutes(securityManager: SecurityManager) extends SecurityRoutes(securityManager) {

  val routes: Route =
    extractCredentials { creds: Option[HttpCredentials] =>
      val subject: Subject = login(creds)
      if (!subject.isAuthenticated) {
        complete(Unauthorized)
      }

      pathPrefix("configs") {
        concat(
          post {
            if (!subject.hasRole(ADMIN.toString) && !subject.hasRole(WRITER.toString)) {
              complete(Forbidden, s"Roles ${ADMIN} or ${WRITER} required")
            }
            ??? //todo: implement
          },
          get {
            if (!subject.hasRole(ADMIN.toString) && !subject.hasRole(READER.toString)) {
              complete(Forbidden, s"Roles ${ADMIN} or ${READER} required")
            }
            ??? //todo: implement
          },
          path(Segment) { collection =>
            concat(
              get {
                if (!subject.hasRole(ADMIN.toString) && !subject.hasRole(READER.toString)) {
                  complete(Forbidden, s"Roles ${ADMIN} or ${READER} required")
                }
                ??? //todo: implement
              },
              delete {
                if (!subject.hasRole(ADMIN.toString) && !subject.hasRole(WRITER.toString)) {
                  complete(Forbidden, s"Roles ${ADMIN} or ${WRITER} required")
                }
                ??? //todo: implement
              }
            )
          }
        )
      }
    }
}
