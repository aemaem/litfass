package lit.fass.server

import akka.actor.testkit.typed.scaladsl.ScalaTestWithActorTestKit
import lit.fass.server.Greeter.{Greet, Greeted}
import org.junit.runner.RunWith
import org.scalatest.wordspec.AnyWordSpecLike
import org.scalatestplus.junit.JUnitRunner

/**
 * @author Michael Mair
 */
@RunWith(classOf[JUnitRunner])
class GreetingsTest extends ScalaTestWithActorTestKit with AnyWordSpecLike {

  "A Greeter" must {
    "reply to greeted" in {
      val replyProbe = createTestProbe[Greeted]()
      val underTest = spawn(Greeter())

      underTest ! Greet("Santa", replyProbe.ref)
      replyProbe.expectMessage(Greeted("Santa", underTest.ref))
    }
  }
}
