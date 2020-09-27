package lit.fass.server.http.route

import akka.http.javadsl.model.HttpRequest.GET
import akka.http.javadsl.model.StatusCodes
import akka.http.javadsl.testkit.JUnitRouteTest
import lit.fass.server.helper.UnitTest
import org.junit.Test
import org.junit.experimental.categories.Category

/**
 * @author Michael Mair
 */
@Category(UnitTest::class)
internal class HealthRoutesTest : JUnitRouteTest() {

    val routeUnderTest = testRoute(HealthRoutes().routes)

    @Test
    fun `health route returns status code 200`() {
        routeUnderTest.run(GET("/health"))
            .assertStatusCode(StatusCodes.OK)
    }
}