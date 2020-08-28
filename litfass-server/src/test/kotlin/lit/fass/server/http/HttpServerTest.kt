package lit.fass.server.http

/**
 * @author Michael Mair
 */
internal class HttpServerTest {
    // todo: implement
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
}