package lit.fass.server.http

import akka.http.scaladsl.model.headers.HttpCredentials
import lit.fass.server.security.SecurityManager
import org.apache.shiro.subject.Subject

/**
 * @author Michael Mair
 */
abstract class SecurityRoutes(securityManager: SecurityManager) {

  protected def login(credentials: Option[HttpCredentials]): Subject = {
    credentials match {
      case Some(value) => securityManager.loginHttpBasic(value.token())
      case _ => securityManager.buildSubject()
    }
  }
}
