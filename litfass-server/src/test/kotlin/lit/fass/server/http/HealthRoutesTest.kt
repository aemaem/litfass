package lit.fass.server.http

import akka.http.javadsl.model.HttpRequest
import akka.http.javadsl.model.StatusCodes.OK
import akka.http.javadsl.testkit.JUnitRouteTest
import org.junit.Test

/**
 * @author Michael Mair
 */
internal class HealthRoutesTest : JUnitRouteTest() {

    val routeUnderTest = testRoute(HealthRoutes().routes)

    @Test
    fun `health route returns status code 200`() {
        routeUnderTest.run(HttpRequest.GET("/health"))
            .assertStatusCode(OK)
    }
}