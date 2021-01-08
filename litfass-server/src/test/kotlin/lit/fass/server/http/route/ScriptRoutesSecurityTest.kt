package lit.fass.server.http.route

import akka.actor.testkit.typed.javadsl.TestKitJunitResource
import akka.actor.typed.ActorRef
import akka.http.javadsl.model.ContentTypes.APPLICATION_JSON
import akka.http.javadsl.model.HttpRequest.POST
import akka.http.javadsl.model.StatusCodes.*
import akka.http.javadsl.model.headers.HttpCredentials.createBasicHttpCredentials
import akka.http.javadsl.testkit.JUnitRouteTest
import akka.http.javadsl.testkit.TestRoute
import io.mockk.MockKAnnotations
import io.mockk.clearAllMocks
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.impl.annotations.MockK
import lit.fass.server.actor.ScriptActor
import lit.fass.server.helper.UnitTest
import lit.fass.server.logger
import lit.fass.server.script.ScriptEngine
import lit.fass.server.script.ScriptLanguage.GROOVY
import lit.fass.server.security.Role
import lit.fass.server.security.Role.ADMIN
import lit.fass.server.security.Role.EXECUTOR
import lit.fass.server.security.SecurityManager
import org.apache.shiro.subject.Subject
import org.junit.Before
import org.junit.ClassRule
import org.junit.Test
import org.junit.experimental.categories.Category
import java.time.Duration.ofSeconds

/**
 * @author Michael Mair
 */
@Category(UnitTest::class)
internal class ScriptRoutesSecurityTest : JUnitRouteTest() {

    companion object {
        @ClassRule
        @JvmField
        val testKit = TestKitJunitResource()
        val log = this.logger()
    }

    lateinit var routeUnderTest: TestRoute

    lateinit var scriptActor: ActorRef<ScriptActor.Message>

    @MockK
    lateinit var securityManagerMock: SecurityManager

    @MockK(relaxed = true)
    lateinit var subjectMock: Subject

    @MockK(relaxed = true)
    lateinit var scriptEngine1Mock: ScriptEngine

    @MockK(relaxed = true)
    lateinit var scriptEngine2Mock: ScriptEngine

    @Before
    fun setup() {
        MockKAnnotations.init(this)
        clearAllMocks()
        every { securityManagerMock.loginHttpBasic(any() as String) } returns subjectMock
        every { scriptEngine1Mock.isApplicable(GROOVY) } returns false
        every { scriptEngine2Mock.isApplicable(GROOVY) } returns true
        @Suppress("UNCHECKED_CAST")
        every { scriptEngine2Mock.invoke("""binding.data""", any()) } answers {
            args[1] as Collection<Map<String, Any?>>
        }

        scriptActor = testKit.spawn(ScriptActor.create(listOf(scriptEngine1Mock, scriptEngine2Mock)))
        routeUnderTest = testRoute(ScriptRoutes(securityManagerMock, scriptActor, testKit.scheduler(), ofSeconds(10)).routes)
    }

    @Test
    fun `script extension test POST endpoint is secured`() {
        every { subjectMock.isAuthenticated } returns false

        routeUnderTest.run(
            POST("/script/groovy/test")
                .withEntity(APPLICATION_JSON, """{"script": "binding.data", "data": {"foo": "bar", "bar": true}}""")
                .addCredentials(createBasicHttpCredentials("", ""))
        ).assertStatusCode(UNAUTHORIZED)
    }

    @Test
    fun `script extension test POST endpoint is permitted for specified roles`() {
        val permittedRoles = setOf(ADMIN, EXECUTOR)
        Role.values().forEach { role ->
            clearMocks(subjectMock)
            every { subjectMock.isAuthenticated } returns true
            log.info("Testing role $role")

            var expectedStatus = FORBIDDEN
            if (role in permittedRoles) {
                expectedStatus = OK
                every { subjectMock.hasRole(role.name) } returns true
                every { subjectMock.principal } returns "user"
            } else {
                every { subjectMock.hasRole(role.name) } returns false
            }

            routeUnderTest.run(
                POST("/script/groovy/test")
                    .withEntity(APPLICATION_JSON, """{"script": "binding.data", "data": {"foo": "bar", "bar": true}}""")
                    .addCredentials(createBasicHttpCredentials("user", "user"))
            ).assertStatusCode(expectedStatus)
        }
    }

}