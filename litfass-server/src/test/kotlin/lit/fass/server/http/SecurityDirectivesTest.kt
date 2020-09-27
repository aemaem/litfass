package lit.fass.server.http

import akka.http.javadsl.model.HttpRequest.GET
import akka.http.javadsl.model.StatusCodes.*
import akka.http.javadsl.model.headers.HttpCredentials.createBasicHttpCredentials
import akka.http.javadsl.server.Route
import akka.http.javadsl.testkit.JUnitRouteTest
import akka.http.javadsl.testkit.TestRoute
import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.impl.annotations.MockK
import lit.fass.server.helper.UnitTest
import lit.fass.server.security.Role.ADMIN
import lit.fass.server.security.Role.EXECUTOR
import lit.fass.server.security.SecurityManager
import org.apache.shiro.subject.Subject
import org.junit.Before
import org.junit.Test
import org.junit.experimental.categories.Category

/**
 * @author Michael Mair
 */
@Category(UnitTest::class)
internal class SecurityDirectivesTest : JUnitRouteTest() {

    lateinit var routeUnderTest: TestRoute

    @MockK
    lateinit var securityManagerMock: SecurityManager

    @MockK(relaxed = true)
    lateinit var subjectMock: Subject

    @Before
    fun setup() {
        MockKAnnotations.init(this)
        every { securityManagerMock.loginHttpBasic(any() as String) } returns subjectMock

        routeUnderTest = testRoute(TestSecurityRoutes(securityManagerMock).routes)
    }

    @Test
    fun `authentication fail`() {
        every { subjectMock.isAuthenticated } returns false

        routeUnderTest.run(GET("/admin-or-executor").addCredentials(createBasicHttpCredentials("", "")))
            .assertStatusCode(UNAUTHORIZED)
    }

    @Test
    fun `authorization fails`() {
        every { subjectMock.isAuthenticated } returns true
        every { subjectMock.hasRole("ADMIN") } returns false

        routeUnderTest.run(GET("/admin-or-executor").addCredentials(createBasicHttpCredentials("", "")))
            .assertStatusCode(FORBIDDEN)
    }

    @Test
    fun `authentication and authorization success`() {
        every { subjectMock.isAuthenticated } returns true
        every { subjectMock.hasRole("ADMIN") } returns true

        routeUnderTest.run(GET("/admin-or-executor").addCredentials(createBasicHttpCredentials("", "")))
            .assertStatusCode(OK)
    }

    class TestSecurityRoutes(securityManager: SecurityManager) : SecurityDirectives(securityManager) {
        val routes: Route =
            authenticate { subject ->
                path("admin-or-executor") {
                    authorize(subject, listOf(ADMIN, EXECUTOR)) {
                        get {
                            complete(OK)
                        }
                    }
                }
            }

    }
}