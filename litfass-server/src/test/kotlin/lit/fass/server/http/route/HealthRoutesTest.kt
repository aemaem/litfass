package lit.fass.server.http.route

import akka.cluster.Member
import akka.cluster.MemberStatus
import akka.cluster.typed.Cluster
import akka.http.javadsl.model.HttpRequest.GET
import akka.http.javadsl.model.StatusCodes
import akka.http.javadsl.testkit.JUnitRouteTest
import akka.http.javadsl.testkit.TestRoute
import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.mockk
import lit.fass.server.helper.UnitTest
import org.junit.Before
import org.junit.Test
import org.junit.experimental.categories.Category

/**
 * @author Michael Mair
 */
@Category(UnitTest::class)
internal class HealthRoutesTest : JUnitRouteTest() {

    lateinit var routeUnderTest: TestRoute

    @MockK(relaxed = true)
    lateinit var clusterMock: Cluster

    @Before
    fun setup() {
        MockKAnnotations.init(this)
        routeUnderTest = testRoute(HealthRoutes(clusterMock).routes)
    }

    @Test
    fun `ready route returns status code 200`() {
        val memberMock = mockk<Member>()
        every { memberMock.status() } returns MemberStatus.up()
        every { clusterMock.selfMember() } returns memberMock

        routeUnderTest.run(GET("/ready"))
            .assertStatusCode(StatusCodes.OK)
    }

    @Test
    fun `health route returns status code 200`() {
        routeUnderTest.run(GET("/health"))
            .assertStatusCode(StatusCodes.OK)
    }
}