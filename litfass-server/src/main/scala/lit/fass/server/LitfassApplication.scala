package lit.fass.server

import akka.actor.typed.ActorSystem
import akka.actor.typed.scaladsl.Behaviors
import lit.fass.server.http.{HttpServer, UserRegistry, UserRoutes}
import lit.fass.server.security.SafeguardManager

/**
 * Main class.
 *
 * @author Michael Mair
 */
object LitfassApplication extends App {
  print("\nLITFASS\n\n")

  val safeguardManager = new SafeguardManager()

  val rootBehavior = Behaviors.setup[Nothing] { context =>
    val userRegistryActor = context.spawn(UserRegistry(), "UserRegistryActor")
    context.watch(userRegistryActor)

    val routes = new UserRoutes(userRegistryActor, safeguardManager)(context.system)
    HttpServer.startHttpServer(routes.userRoutes, context.system)

    Behaviors.empty
  }
  val system = ActorSystem[Nothing](rootBehavior, "litfass")
}
