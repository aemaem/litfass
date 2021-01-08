package lit.fass.server.http.route

import akka.actor.testkit.typed.javadsl.TestKitJunitResource
import akka.actor.typed.ActorRef
import akka.http.javadsl.marshallers.jackson.Jackson.unmarshaller
import akka.http.javadsl.model.ContentTypes.APPLICATION_JSON
import akka.http.javadsl.model.HttpHeader
import akka.http.javadsl.model.HttpRequest.GET
import akka.http.javadsl.model.HttpRequest.POST
import akka.http.javadsl.model.StatusCodes.OK
import akka.http.javadsl.model.headers.HttpCredentials
import akka.http.javadsl.testkit.JUnitRouteTest
import akka.http.javadsl.testkit.TestRoute
import io.mockk.MockKAnnotations
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.verify
import lit.fass.server.actor.CollectionActor
import lit.fass.server.config.ConfigService
import lit.fass.server.config.yaml.model.CollectionConfig
import lit.fass.server.execution.ExecutionService
import lit.fass.server.helper.UnitTest
import lit.fass.server.persistence.CollectionPersistenceService
import lit.fass.server.persistence.Datastore.POSTGRES
import lit.fass.server.security.SecurityManager
import org.apache.shiro.subject.Subject
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.ClassRule
import org.junit.Test
import org.junit.experimental.categories.Category
import java.time.Duration.ofSeconds

/**
 * @author Michael Mair
 */
@Category(UnitTest::class)
internal class CollectionRoutesFunctionTest : JUnitRouteTest() {

    companion object {
        @ClassRule
        @JvmField
        val testKit = TestKitJunitResource()
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
        every { subjectMock.isAuthenticated } returns true
        every { subjectMock.hasRole(any()) } returns true
        every { subjectMock.principal } returns "admin"
        every { securityManagerMock.loginHttpBasic(any() as String) } returns subjectMock
        collectionActor = testKit.spawn(
            CollectionActor.create(configServiceMock, executionServiceMock, listOf(persistenceService1Mock, persistenceService2Mock))
        )
        routeUnderTest = testRoute(CollectionRoutes(securityManagerMock, collectionActor, testKit.scheduler(), ofSeconds(10)).routes)
    }

    @Test
    fun `collections {collection} {id} GET endpoint`() {
        every { configServiceMock.getConfig("foo") } returns CollectionConfig("foo", null, null, POSTGRES, emptyList())
        every { persistenceService1Mock.isApplicable(POSTGRES) } returns true
        every { persistenceService2Mock.isApplicable(POSTGRES) } returns false
        every { persistenceService1Mock.findCollectionData("foo", "1") } returns mapOf(
            "foo" to 100,
            "bar" to listOf("the", "only", "way", "is one")
        )

        routeUnderTest.run(
            GET("/collections/foo/1")
                .addCredentials(HttpCredentials.createBasicHttpCredentials("user", "user"))
        ).assertStatusCode(OK)
            .entity(unmarshaller(Map::class.java)).run {
                val it = this
                assertThat(it["foo"]).isEqualTo(100)
                assertThat(it["bar"] as List<*>).hasSize(4)
            }
    }

    @Test
    fun `collections {collection} POST endpoint`() {
        every { configServiceMock.getConfig("foo") } returns CollectionConfig("foo", null, null, POSTGRES, emptyList())

        routeUnderTest.run(
            POST("/collections/foo?bar=true&foo=42")
                .addCredentials(HttpCredentials.createBasicHttpCredentials("user", "user"))
                .addHeader(HttpHeader.parse("x-foo", "bar"))
                .withEntity(
                    APPLICATION_JSON, """
                    {"bla": true, "blub": [1,2,3]}
                """.trimIndent().toByteArray()
                )
        ).assertStatusCode(OK)

        verify(exactly = 1) {
            executionServiceMock.execute(any(), match {
                assertThat(it).hasSize(1)
                assertThat(it.first()).hasSize(7)
                assertThat(it.first()["bar"]).isEqualTo("true")
                assertThat(it.first()["foo"]).isEqualTo("42")
                assertThat(it.first()["x-foo"]).isEqualTo("bar")
                assertThat(it.first()["bla"] as Boolean).isTrue()
                assertThat(it.first()["blub"] as List<*>).hasSize(3)
                true
            })
        }
    }

    @Test
    fun `collections {collection} GET endpoint`() {
        every { configServiceMock.getConfig("foo") } returns CollectionConfig("foo", null, null, POSTGRES, emptyList())

        routeUnderTest.run(
            GET("/collections/foo?bar=true&foo=42")
                .addCredentials(HttpCredentials.createBasicHttpCredentials("user", "user"))
                .addHeader(HttpHeader.parse("x-foo", "bar"))
        ).assertStatusCode(OK)

        verify(exactly = 1) {
            executionServiceMock.execute(any(), match {
                assertThat(it).hasSize(1)
                assertThat(it.first()).hasSize(5)
                assertThat(it.first()["bar"]).isEqualTo("true")
                assertThat(it.first()["foo"]).isEqualTo("42")
                assertThat(it.first()["x-foo"]).isEqualTo("bar")
                true
            })
        }
    }

}