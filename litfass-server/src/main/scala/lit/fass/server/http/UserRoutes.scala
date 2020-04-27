package lit.fass.server.http

import akka.actor.typed.scaladsl.AskPattern._
import akka.actor.typed.{ActorRef, ActorSystem}
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
import akka.http.scaladsl.model.StatusCodes.{Created, OK}
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.server.directives.Credentials
import akka.util.Timeout
import lit.fass.server.http.JsonFormats._
import lit.fass.server.http.UserRegistry._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class UserRoutes(userRegistry: ActorRef[UserRegistry.Command])(implicit val system: ActorSystem[_]) {

  private implicit val timeout: Timeout = Timeout.create(system.settings.config.getDuration("litfass.routes.ask-timeout"))

  def defaultAuthenticator(credentials: Credentials): Future[Option[String]] =
    credentials match {
      case p@Credentials.Provided(id) =>
        Future {
          //todo: get users from config
          if (p.verify("p4ssw0rd")) Some(id)
          else None
        }
      case _ => Future.successful(None)
    }

  def getUsers(): Future[Users] =
    userRegistry.ask(GetUsers)

  def getUser(name: String): Future[GetUserResponse] =
    userRegistry.ask(GetUser(name, _))

  def createUser(user: User): Future[ActionPerformed] =
    userRegistry.ask(CreateUser(user, _))

  def deleteUser(name: String): Future[ActionPerformed] =
    userRegistry.ask(DeleteUser(name, _))

  val userRoutes: Route =
    pathPrefix("users") {
      concat(
        pathEnd {
          concat(
            get {
              complete(getUsers())
            },
            post {
              entity(as[User]) { user =>
                onSuccess(createUser(user)) { performed =>
                  complete((Created, performed))
                }
              }
            }
          )
        },
        path(Segment) { name =>
          concat(
            get {
              rejectEmptyResponse {
                onSuccess(getUser(name)) { response =>
                  complete(response.maybeUser)
                }
              }
            },
            delete {
              onSuccess(deleteUser(name)) { performed =>
                complete((OK, performed))
              }
            }
          )
        },
        pathPrefix("greet") {
          authenticateBasicAsync("basic", defaultAuthenticator) { userName =>
            get {
              authorizeAsync(_ => Future.successful(true)) { //todo: implement Shiro Authorization
                complete((OK, s"Servus $userName!"))
              }
            }
          }
        }
      )
    }
}
