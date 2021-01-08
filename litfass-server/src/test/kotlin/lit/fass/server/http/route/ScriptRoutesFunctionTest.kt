package lit.fass.server.http.route

import akka.actor.testkit.typed.javadsl.TestKitJunitResource
import akka.actor.typed.ActorRef
import akka.http.javadsl.marshallers.jackson.Jackson.unmarshaller
import akka.http.javadsl.model.ContentTypes.APPLICATION_JSON
import akka.http.javadsl.model.HttpRequest.POST
import akka.http.javadsl.model.StatusCodes.OK
import akka.http.javadsl.model.headers.HttpCredentials.createBasicHttpCredentials
import akka.http.javadsl.testkit.JUnitRouteTest
import akka.http.javadsl.testkit.TestRoute
import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.impl.annotations.MockK
import lit.fass.server.actor.ConfigActor
import lit.fass.server.actor.ScriptActor
import lit.fass.server.helper.UnitTest
import lit.fass.server.script.ScriptEngine
import lit.fass.server.script.ScriptLanguage
import lit.fass.server.security.Role.ADMIN
import lit.fass.server.security.Role.EXECUTOR
import lit.fass.server.security.SecurityManager
import org.apache.shiro.subject.Subject
import org.junit.Before
import org.junit.ClassRule
import org.junit.Test
import org.junit.experimental.categories.Category
import java.time.Duration
import java.time.Duration.ofSeconds

/**
 * @author Michael Mair
 */
@Category(UnitTest::class)
internal class ScriptRoutesFunctionTest : JUnitRouteTest() {

    companion object {
        @ClassRule
        @JvmField
        val testKit = TestKitJunitResource()
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
        every { subjectMock.isAuthenticated } returns true
        every { subjectMock.hasRole(any()) } returns true
        every { subjectMock.principal } returns "admin"
        every { securityManagerMock.loginHttpBasic(any() as String) } returns subjectMock
        every { scriptEngine1Mock.isApplicable(ScriptLanguage.GROOVY) } returns false
        every { scriptEngine2Mock.isApplicable(ScriptLanguage.GROOVY) } returns true
        @Suppress("UNCHECKED_CAST")
        every { scriptEngine2Mock.invoke("""binding.data""", any()) } answers {
            args[1] as Collection<Map<String, Any?>>
        }

        scriptActor = testKit.spawn(ScriptActor.create(listOf(scriptEngine1Mock, scriptEngine2Mock)))
        routeUnderTest = testRoute(ScriptRoutes(securityManagerMock, scriptActor, testKit.scheduler(), ofSeconds(10)).routes)
    }

    @Test
    fun `script extension test POST endpoint returns result`() {
        routeUnderTest.run(
            POST("/script/groovy/test")
                .withEntity(
                    APPLICATION_JSON,
                    """{
                        "script": "binding.data",
                        "data": {"foo": "bar", "bar": true}
                    }""".trimIndent()
                )
                .addCredentials(createBasicHttpCredentials("admin", "admin"))
        )
            .assertStatusCode(OK)
            .assertEntityAs(
                unmarshaller(List::class.java), listOf(
                    mapOf("foo" to "bar", "bar" to true)
                )
            )
    }

}