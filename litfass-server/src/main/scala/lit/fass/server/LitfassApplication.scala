package lit.fass.server

import akka.actor.typed.ActorSystem
import akka.actor.typed.scaladsl.Behaviors
import akka.http.scaladsl.server.Directives._
import lit.fass.server.http._
import lit.fass.server.security.SecurityManager

/**
 * Main class.
 *
 * @author Michael Mair
 */
object LitfassApplication extends App {
  print("\nLITFASS\n\n")

  val rootBehavior = Behaviors.setup[Nothing] { context =>
    val securityManager = SecurityManager(context.system.settings.config)

    val userRegistryActor = context.spawn(UserRegistry(), "UserRegistryActor")
    context.watch(userRegistryActor)

    HttpServer.startHttpServer(concat(
      HealthRoutes().routes,
      CollectionRoutes(securityManager).routes,
      ConfigRoutes(securityManager).routes,
      new UserRoutes(userRegistryActor, securityManager)(context.system).userRoutes //todo: remove
    ), context.system)

    Behaviors.empty
  }
  val system = ActorSystem[Nothing](rootBehavior, "litfass")
}
