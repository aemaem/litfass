package lit.fass.server.http.route

import akka.actor.testkit.typed.javadsl.TestKitJunitResource
import akka.actor.typed.ActorRef
import akka.http.javadsl.model.HttpRequest.GET
import akka.http.javadsl.model.StatusCodes
import akka.http.javadsl.model.StatusCodes.FORBIDDEN
import akka.http.javadsl.model.StatusCodes.OK
import akka.http.javadsl.model.headers.HttpCredentials
import akka.http.javadsl.testkit.JUnitRouteTest
import akka.http.javadsl.testkit.TestRoute
import io.mockk.MockKAnnotations
import io.mockk.clearAllMocks
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.impl.annotations.MockK
import lit.fass.server.actor.CollectionActor
import lit.fass.server.config.ConfigService
import lit.fass.server.config.yaml.model.CollectionConfig
import lit.fass.server.execution.ExecutionService
import lit.fass.server.helper.UnitTest
import lit.fass.server.logger
import lit.fass.server.persistence.CollectionPersistenceService
import lit.fass.server.persistence.Datastore.POSTGRES
import lit.fass.server.security.Role
import lit.fass.server.security.Role.ADMIN
import lit.fass.server.security.Role.READER
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
internal class CollectionRoutesSecurityTest : JUnitRouteTest() {

    companion object {
        @ClassRule
        @JvmField
        val testKit = TestKitJunitResource()
        val log = this.logger()
    }

    lateinit var routeUnderTest: TestRoute

    lateinit var collectionActor: ActorRef<CollectionActor.Message>

    @MockK
    lateinit var securityManagerMock: SecurityManager

    @MockK(relaxed = true)
    lateinit var subjectMock: Subject

    @MockK(relaxed = true)
    lateinit var configServiceMock: ConfigService

    @MockK(relaxed = true)
    lateinit var executionServiceMock: ExecutionService

    @MockK(relaxed = true)
    lateinit var persistenceService1Mock: CollectionPersistenceService

    @MockK(relaxed = true)
    lateinit var persistenceService2Mock: CollectionPersistenceService

    @Before
    fun setup() {
        MockKAnnotations.init(this)
        clearAllMocks()
        every { securityManagerMock.loginHttpBasic(any() as String) } returns subjectMock
        every { configServiceMock.getConfig("foo") } returns CollectionConfig("foo", null, null, POSTGRES, emptyList())
        every { persistenceService1Mock.isApplicable(POSTGRES) } returns true
        every { persistenceService2Mock.isApplicable(POSTGRES) } returns false
        every { persistenceService1Mock.findCollectionData("foo", "1") } returns emptyMap()
        collectionActor = testKit.spawn(
            CollectionActor.create(configServiceMock, executionServiceMock, listOf(persistenceService1Mock, persistenceService2Mock))
        )
        routeUnderTest = testRoute(CollectionRoutes(securityManagerMock, collectionActor, testKit.scheduler(), ofSeconds(10)).routes)
    }

    @Test
    fun `collections {collection} with id GET endpoint is secured`() {
        every { subjectMock.isAuthenticated } returns false

        routeUnderTest.run(GET("/collections/foo/1").addCredentials(HttpCredentials.createBasicHttpCredentials("", "")))
            .assertStatusCode(StatusCodes.UNAUTHORIZED)
    }

    @Test
    fun `collections {collection} {id} GET endpoint is permitted for specified roles`() {
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
                GET("/collections/foo/1")
                    .addCredentials(HttpCredentials.createBasicHttpCredentials("user", "user"))
            ).assertStatusCode(expectedStatus)
        }
    }

}