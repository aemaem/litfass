package lit.fass.server.http.route

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
import lit.fass.server.helper.UnitTest
import lit.fass.server.script.ScriptEngine
import lit.fass.server.script.ScriptLanguage
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
internal class ScriptRoutesFunctionTest : JUnitRouteTest() {

    lateinit var routeUnderTest: TestRoute

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
        every { subjectMock.hasRole(or(ADMIN.name, EXECUTOR.name)) } returns true
        every { subjectMock.principal } returns "admin"
        every { securityManagerMock.loginHttpBasic(any() as String) } returns subjectMock
        every { scriptEngine1Mock.isApplicable(ScriptLanguage.GROOVY) } returns false
        every { scriptEngine2Mock.isApplicable(ScriptLanguage.GROOVY) } returns true
        @Suppress("UNCHECKED_CAST")
        every { scriptEngine2Mock.invoke("""binding.data""", any()) } answers {
            args[1] as Collection<Map<String, Any?>>
        }

        routeUnderTest = testRoute(ScriptRoutes(securityManagerMock, listOf(scriptEngine1Mock, scriptEngine2Mock)).routes)
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