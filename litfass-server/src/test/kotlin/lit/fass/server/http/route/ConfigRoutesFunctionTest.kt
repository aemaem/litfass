package lit.fass.server.http.route

import akka.http.javadsl.marshallers.jackson.Jackson.unmarshaller
import akka.http.javadsl.model.ContentTypes.TEXT_PLAIN_UTF8
import akka.http.javadsl.model.HttpRequest.*
import akka.http.javadsl.model.StatusCodes.NO_CONTENT
import akka.http.javadsl.model.StatusCodes.OK
import akka.http.javadsl.model.headers.HttpCredentials.createBasicHttpCredentials
import akka.http.javadsl.testkit.JUnitRouteTest
import akka.http.javadsl.testkit.TestRoute
import io.mockk.MockKAnnotations
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.impl.annotations.MockK
import lit.fass.server.config.ConfigService
import lit.fass.server.config.yaml.model.CollectionConfig
import lit.fass.server.config.yaml.model.CollectionFlowConfig
import lit.fass.server.helper.UnitTest
import lit.fass.server.security.SecurityManager
import org.apache.shiro.subject.Subject
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.experimental.categories.Category

/**
 * @author Michael Mair
 */
@Category(UnitTest::class)
internal class ConfigRoutesFunctionTest : JUnitRouteTest() {

    lateinit var routeUnderTest: TestRoute

    @MockK
    lateinit var securityManagerMock: SecurityManager

    @MockK(relaxed = true)
    lateinit var subjectMock: Subject

    @MockK(relaxed = true)
    lateinit var configServiceMock: ConfigService

    @Before
    fun setup() {
        MockKAnnotations.init(this)
        clearAllMocks()
        every { subjectMock.isAuthenticated } returns true
        every { subjectMock.hasRole(any()) } returns true
        every { subjectMock.principal } returns "admin"
        every { securityManagerMock.loginHttpBasic(any() as String) } returns subjectMock
        every { configServiceMock.getConfigs() } returns listOf(
            CollectionConfig("foo", null, null, flows = listOf(CollectionFlowConfig(null, null, steps = emptyList()))),
            CollectionConfig("bar", null, null, flows = listOf(CollectionFlowConfig(null, null, steps = emptyList())))
        )
        every { configServiceMock.getConfig("foo") } returns
                CollectionConfig("foo", null, null, flows = listOf(CollectionFlowConfig(null, null, steps = emptyList())))
        routeUnderTest = testRoute(ConfigRoutes(securityManagerMock, configServiceMock).routes)
    }

    @Test
    fun `configs POST endpoint`() {
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
                                    description: "Transform something"
                                    language: kotlin
                                    code: println("bar")
                        """.trimIndent().toByteArray()
                )
                .addCredentials(createBasicHttpCredentials("admin", "admin"))
        ).assertStatusCode(NO_CONTENT)
    }

    @Test
    fun `configs GET endpoint`() {
        routeUnderTest.run(
            GET("/configs")
                .addCredentials(createBasicHttpCredentials("admin", "admin"))
        ).assertStatusCode(OK)
            .entity(unmarshaller(List::class.java)).run {
                @Suppress("UNCHECKED_CAST")
                val it = this as List<Map<String, Any?>>
                assertThat(it).hasSize(2)
                assertThat(it[0]["collection"]).isEqualTo("foo")
                assertThat(it[0]["flows"]).isNotNull
                assertThat(it[1]["collection"]).isEqualTo("bar")
                assertThat(it[1]["flows"]).isNotNull
            }
    }

    @Test
    fun `configs {collection} GET endpoint`() {
        routeUnderTest.run(
            GET("/configs/foo")
                .addCredentials(createBasicHttpCredentials("admin", "admin"))
        ).assertStatusCode(OK)
            .entity(unmarshaller(Map::class.java)).run {
                val it = this
                assertThat(it["collection"]).isEqualTo("foo")
                assertThat(it["flows"] is Collection<*>).isTrue()
                assertThat(it["flows"] as Collection<*>).hasSize(1)
            }
    }

    @Test
    fun `configs {collection} DELETE endpoint`() {
        routeUnderTest.run(
            DELETE("/configs/foo")
                .addCredentials(createBasicHttpCredentials("admin", "admin"))
        ).assertStatusCode(NO_CONTENT)
    }
}