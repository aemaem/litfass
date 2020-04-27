package lit.fass.server

import akka.actor.typed.ActorSystem
import akka.actor.typed.scaladsl.Behaviors
import lit.fass.server.GreeterMain.SayHello
import lit.fass.server.http.{HttpServer, UserRegistry, UserRoutes}

/**
 * Main class.
 *
 * @author Michael Mair
 */
object LitfassApplication extends App {
  print("\nLITFASS\n\n")

//  val greeterMain: ActorSystem[GreeterMain.SayHello] = ActorSystem(GreeterMain(), "litfass")
//  greeterMain ! SayHello("Sepp")

  val rootBehavior = Behaviors.setup[Nothing] { context =>
    val userRegistryActor = context.spawn(UserRegistry(), "UserRegistryActor")
    context.watch(userRegistryActor)

    val routes = new UserRoutes(userRegistryActor)(context.system)
    HttpServer.startHttpServer(routes.userRoutes, context.system)

    Behaviors.empty
  }
  val system = ActorSystem[Nothing](rootBehavior, "litfass")
}
