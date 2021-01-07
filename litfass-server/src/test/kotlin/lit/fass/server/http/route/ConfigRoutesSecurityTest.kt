package lit.fass.server.http.route

import akka.actor.testkit.typed.javadsl.TestKitJunitResource
import akka.http.javadsl.model.ContentTypes.TEXT_PLAIN_UTF8
import akka.http.javadsl.model.HttpRequest.*
import akka.http.javadsl.model.StatusCodes.*
import akka.http.javadsl.model.headers.HttpCredentials.createBasicHttpCredentials
import akka.http.javadsl.testkit.JUnitRouteTest
import akka.http.javadsl.testkit.TestRoute
import io.mockk.MockKAnnotations
import io.mockk.clearAllMocks
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.impl.annotations.MockK
import lit.fass.server.actor.ConfigActor
import lit.fass.server.actor.SchedulerActor
import lit.fass.server.config.ConfigService
import lit.fass.server.config.yaml.model.CollectionConfig
import lit.fass.server.helper.UnitTest
import lit.fass.server.logger
import lit.fass.server.schedule.SchedulerService
import lit.fass.server.security.Role
import lit.fass.server.security.Role.*
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
internal class ConfigRoutesSecurityTest : JUnitRouteTest() {

    companion object {
        @ClassRule
        @JvmField
        val testKit = TestKitJunitResource()
        val log = this.logger()
    }

    lateinit var routeUnderTest: TestRoute

    @MockK
    lateinit var securityManagerMock: SecurityManager

    @MockK(relaxed = true)
    lateinit var subjectMock: Subject

    @MockK(relaxed = true)
    lateinit var configServiceMock: ConfigService

    @MockK(relaxed = true)
    lateinit var schedulerServiceMock: SchedulerService

    @Before
    fun setup() {
        MockKAnnotations.init(this)
        clearAllMocks()
        every { securityManagerMock.loginHttpBasic(any() as String) } returns subjectMock
        every { configServiceMock.getConfigs() } returns emptyList()
        every { configServiceMock.getConfig("foo") } returns CollectionConfig("foo", null, null, flows = emptyList())

        val schedulerActor = testKit.spawn(SchedulerActor.create(schedulerServiceMock))
        val configActor = testKit.spawn(ConfigActor.create(schedulerActor, configServiceMock, ofSeconds(10)))
        routeUnderTest = testRoute(ConfigRoutes(securityManagerMock, configActor, testKit.scheduler(), ofSeconds(10)).routes)
    }

    @Test
    fun `configs POST endpoint is secured`() {
        every { subjectMock.isAuthenticated } returns false

        routeUnderTest.run(POST("/configs").addCredentials(createBasicHttpCredentials("", "")))
            .assertStatusCode(UNAUTHORIZED)
    }

    @Test
    fun `configs POST endpoint is permitted for specified roles`() {
        val permittedRoles = setOf(ADMIN, WRITER)
        Role.values().forEach { role ->
            clearMocks(subjectMock)
            every { subjectMock.isAuthenticated } returns true
            log.info("Testing role $role")

            var expectedStatus = FORBIDDEN
            if (role in permittedRoles) {
                expectedStatus = NO_CONTENT
                every { subjectMock.hasRole(role.name) } returns true
                every { subjectMock.principal } returns "user"
            } else {
                every { subjectMock.hasRole(role.name) } returns false
            }

            routeUnderTest.run(
                POST("/configs")
                    .withEntity(
                        TEXT_PLAIN_UTF8,
                        """
                        collection: foo
                        flows:
                          - flow:
                              steps:
                                - script:
                                    language: kotlin
                                    code: println("bar")
                        """.trimIndent().toByteArray()
                    )
                    .addCredentials(createBasicHttpCredentials("user", "user"))
            ).assertStatusCode(expectedStatus)
        }
    }

    @Test
    fun `configs GET endpoint is secured`() {
        every { subjectMock.isAuthenticated } returns false

        routeUnderTest.run(GET("/configs").addCredentials(createBasicHttpCredentials("", "")))
            .assertStatusCode(UNAUTHORIZED)
    }

    @Test
    fun `configs GET endpoint is permitted for specified roles`() {
        val permittedRoles = setOf(ADMIN, READER)
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
                GET("/configs")
                    .addCredentials(createBasicHttpCredentials("user", "user"))
            ).assertStatusCode(expectedStatus)
        }
    }

    @Test
    fun `configs GET single endpoint is secured`() {
        every { subjectMock.isAuthenticated } returns false

        routeUnderTest.run(GET("/configs").addCredentials(createBasicHttpCredentials("", "")))
            .assertStatusCode(UNAUTHORIZED)
    }

    @Test
    fun `configs GET single endpoint is permitted for specified roles`() {
        val permittedRoles = setOf(ADMIN, READER)
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
                GET("/configs/foo")
                    .addCredentials(createBasicHttpCredentials("user", "user"))
            ).assertStatusCode(expectedStatus)
        }
    }

    @Test
    fun `configs DELETE endpoint is secured`() {
        every { subjectMock.isAuthenticated } returns false

        routeUnderTest.run(DELETE("/configs/foo").addCredentials(createBasicHttpCredentials("", "")))
            .assertStatusCode(UNAUTHORIZED)
    }

    @Test
    fun `configs DELETE endpoint is permitted for specified roles`() {
        val permittedRoles = setOf(ADMIN, WRITER)
        Role.values().forEach { role ->
            clearMocks(subjectMock)
            every { subjectMock.isAuthenticated } returns true
            log.info("Testing role $role")

            var expectedStatus = FORBIDDEN
            if (role in permittedRoles) {
                expectedStatus = NO_CONTENT
                every { subjectMock.hasRole(role.name) } returns true
                every { subjectMock.principal } returns "user"
            } else {
                every { subjectMock.hasRole(role.name) } returns false
            }

            routeUnderTest.run(
                DELETE("/configs/foo")
                    .addCredentials(createBasicHttpCredentials("user", "user"))
            ).assertStatusCode(expectedStatus)
        }
    }
}