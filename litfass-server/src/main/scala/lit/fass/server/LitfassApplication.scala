package lit.fass.server

import akka.actor.typed.ActorSystem
import lit.fass.server.GreeterMain.SayHello

/**
 * Main class.
 *
 * @author Michael Mair
 */
object LitfassApplication extends App {
  print("\nLITFASS\n\n")

  val greeterMain: ActorSystem[GreeterMain.SayHello] = ActorSystem(GreeterMain(), "litfass")
  greeterMain ! SayHello("Sepp")
}
