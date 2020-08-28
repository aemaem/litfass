//package lit.fass.server.http
//
//import akka.http.scaladsl.model.StatusCodes.OK
//import akka.http.scaladsl.server.Route
//import akka.http.scaladsl.testkit.ScalatestRouteTest
//import lit.fass.server.helper.UnitTest
//import org.junit.experimental.categories.Category
//import org.junit.runner.RunWith
//import org.scalatest.concurrent.ScalaFutures
//import org.scalatest.matchers.should.Matchers
//import org.scalatest.wordspec.AnyWordSpec
//import org.scalatestplus.junit.JUnitRunner
//
///**
// * @author Michael Mair
// */
//@Deprecated
//@RunWith(classOf[JUnitRunner])
//@Category(Array(classOf[UnitTest]))
//class HealthRoutesTest extends AnyWordSpec with Matchers with ScalaFutures with ScalatestRouteTest {
//
////  val routes: Route = HealthRoutes().routes
////
////  "health routes" should {
////    "return status code 200" in {
////      Get("/health") ~> routes ~> check {
////        status shouldEqual OK
////      }
////    }
////  }
//}
