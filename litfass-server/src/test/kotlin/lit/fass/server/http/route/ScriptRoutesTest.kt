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
import io.mockk.mockk
import lit.fass.server.helper.UnitTest
import lit.fass.server.script.ScriptEngine
import lit.fass.server.script.ScriptLanguage
import lit.fass.server.security.SecurityManager
import org.apache.shiro.subject.Subject
import org.junit.Before
import org.junit.Test
import org.junit.experimental.categories.Category

/**
 * @author Michael Mair
 */
@Category(UnitTest::class)
internal class ScriptRoutesTest : JUnitRouteTest() {

    lateinit var routeUnderTest: TestRoute

    @MockK
    lateinit var securityManagerMock: SecurityManager

    @MockK(relaxed = true)
    lateinit var scriptEngine1Mock: ScriptEngine

    @MockK(relaxed = true)
    lateinit var scriptEngine2Mock: ScriptEngine

    @Before
    fun setup() {
        MockKAnnotations.init(this)
        val authenticatedSubjectMock = mockk<Subject>()
        every { authenticatedSubjectMock.isAuthenticated } returns true
        every { authenticatedSubjectMock.hasRole("ADMIN") } returns true
        every { authenticatedSubjectMock.principal } returns "admin"
        every { securityManagerMock.loginHttpBasic(any() as String) } returns authenticatedSubjectMock
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