//package lit.fass.server
//
//import akka.actor.testkit.typed.scaladsl.ScalaTestWithActorTestKit
//import lit.fass.server.Greeter.{Greet, Greeted}
//import lit.fass.server.helper.UnitTest
//import org.junit.experimental.categories.Category
//import org.junit.runner.RunWith
//import org.scalatest.matchers.should.Matchers
//import org.scalatest.wordspec.AnyWordSpecLike
//import org.scalatestplus.junit.JUnitRunner
//
///**
// * @author Michael Mair
// */
//@RunWith(classOf[JUnitRunner])
//@Category(Array(classOf[UnitTest]))
//class GreetingsTest extends ScalaTestWithActorTestKit with AnyWordSpecLike with Matchers {
//
//  "A Greeter" must {
//    "reply to greeted" in {
//      val replyProbe = createTestProbe[Greeted]()
//      val underTest = spawn(Greeter())
//
//      underTest ! Greet("Santa", replyProbe.ref)
//      replyProbe.expectMessage(Greeted("Santa", underTest.ref))
//    }
//  }
//}
