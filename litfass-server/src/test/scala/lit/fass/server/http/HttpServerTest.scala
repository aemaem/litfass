//package lit.fass.server.http
//
//import akka.actor.testkit.typed.scaladsl.ActorTestKit
//import akka.actor.typed.scaladsl.adapter._
//import akka.actor.typed.{ActorRef, ActorSystem}
//import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
//import akka.http.scaladsl.marshalling.Marshal
//import akka.http.scaladsl.model.ContentTypes.`application/json`
//import akka.http.scaladsl.model.StatusCodes.{Created, OK}
//import akka.http.scaladsl.model.headers.BasicHttpCredentials
//import akka.http.scaladsl.model.{HttpRequest, MessageEntity}
//import akka.http.scaladsl.server.{AuthenticationFailedRejection, Route}
//import akka.http.scaladsl.testkit.ScalatestRouteTest
//import lit.fass.server.helper.UnitTest
//import lit.fass.server.http.JsonFormats._
//import lit.fass.server.security.SecurityManager
//import org.junit.experimental.categories.Category
//import org.junit.runner.RunWith
//import org.scalatest.Ignore
//import org.scalatest.concurrent.ScalaFutures
//import org.scalatest.matchers.should.Matchers
//import org.scalatest.wordspec.AnyWordSpec
//import org.scalatestplus.junit.JUnitRunner
//
///**
// * @author Michael Mair
// */
//@Ignore
//@RunWith(classOf[JUnitRunner])
//@Category(Array(classOf[UnitTest]))
//class HttpServerTest extends AnyWordSpec with Matchers with ScalaFutures with ScalatestRouteTest {
//
//  lazy val testKit: ActorTestKit = ActorTestKit()
//
//  implicit def typedSystem: ActorSystem[Nothing] = testKit.system
//
//  override def createActorSystem(): akka.actor.ActorSystem = testKit.system.toClassic
//
//  val userRegistry: ActorRef[UserRegistry.Command] = testKit.spawn(UserRegistry())
//  val safeguardManager = new SecurityManager()
//  lazy val routes: Route = new UserRoutes(userRegistry, safeguardManager).userRoutes
//
//
//  "UserRoutes" should {
//    "return unauthorized if not authenticated (GET /users/greet)" in {
//      Get("/users/greet") ~> routes ~> check {
//        rejection shouldBe an[AuthenticationFailedRejection]
//      }
//    }
//
//    "return unauthorized if authorization fails (GET /users/greet)" in {
//      Get("/users/greet") ~> addCredentials(BasicHttpCredentials("Sepp", "p4ffw0rd")) ~> routes ~> check {
//        rejection shouldBe an[AuthenticationFailedRejection]
//      }
//    }
//
//    "return greeting if authenticated (GET /users/greet)" in {
//      Get("/users/greet") ~> addCredentials(BasicHttpCredentials("Sepp", "p4ssw0rd")) ~> routes ~> check {
//        status shouldEqual OK
//        responseAs[String] shouldEqual "Servus Sepp!"
//      }
//    }
//
//    "return no users if no present (GET /users)" in {
//      // note that there's no need for the host part in the uri:
//      val request = HttpRequest(uri = "/users")
//
//      request ~> routes ~> check {
//        status shouldEqual OK
//
//        // we expect the response to be json:
//        contentType shouldEqual `application/json`
//
//        // and no entries should be in the list:
//        entityAs[String] shouldEqual """{"users":[]}"""
//      }
//    }
//
//    "be able to add users (POST /users)" in {
//      val user = User("Kapi", 42, "jp")
//      val userEntity = Marshal(user).to[MessageEntity].futureValue // futureValue is from ScalaFutures
//
//      // using the RequestBuilding DSL:
//      val request = Post("/users").withEntity(userEntity)
//
//      request ~> routes ~> check {
//        status shouldEqual Created
//
//        // we expect the response to be json:
//        contentType shouldEqual `application/json`
//
//        // and we know what message we're expecting back:
//        entityAs[String] shouldEqual """{"description":"User Kapi created."}"""
//      }
//    }
//
//    "be able to remove users (DELETE /users)" in {
//      // user the RequestBuilding DSL provided by ScalatestRouteSpec:
//      val request = Delete(uri = "/users/Kapi")
//
//      request ~> routes ~> check {
//        status shouldEqual OK
//
//        // we expect the response to be json:
//        contentType shouldEqual `application/json`
//
//        // and no entries should be in the list:
//        entityAs[String] shouldEqual """{"description":"User Kapi deleted."}"""
//      }
//    }
//  }
//}
//
