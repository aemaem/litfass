package lit.fass.server.http

import akka.actor.typed.scaladsl.AskPattern._
import akka.actor.typed.{ActorRef, ActorSystem}
import akka.http.scaladsl.model.StatusCodes.{Forbidden, Unauthorized}
import akka.http.scaladsl.model.headers.HttpCredentials
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.util.Timeout
import lit.fass.server.script.ScriptActor.{ExecuteScriptMessage, ScriptMessage}
import lit.fass.server.security.Role.{ADMIN, EXECUTER}
import lit.fass.server.security.SecurityManager
import org.apache.shiro.subject.Subject

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future


/**
 * @author Michael Mair
 */
class ScriptRoutes(actor: ActorRef[ScriptMessage],
                   securityManager: SecurityManager)(implicit val system: ActorSystem[_]) extends SecurityRoutes(securityManager) {

  private implicit val timeout: Timeout = Timeout.create(system.settings.config.getDuration("litfass.routes.ask-timeout"))

  val routes: Route =
    extractCredentials { creds: Option[HttpCredentials] =>
      val subject: Subject = login(creds)
      if (!subject.isAuthenticated) {
        complete(Unauthorized)
      }

      pathPrefix("script") {
        path(Segment) { language =>
          pathPrefix("test") {
            post {
              if (!subject.hasRole(ADMIN.toString) && !subject.hasRole(EXECUTER.toString)) {
                complete(Forbidden, s"Roles ${ADMIN} or ${EXECUTER} required")
              }
              //todo: get script
              //todo: get data
              complete(actor.ask(ExecuteScriptMessage(language, null, null)))
            }
          }
        }
      }
    }
}
