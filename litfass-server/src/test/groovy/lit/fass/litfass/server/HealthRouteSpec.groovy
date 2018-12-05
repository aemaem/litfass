package lit.fass.litfass.server

import groovy.json.JsonSlurper
import lit.fass.litfass.server.helper.IntegrationTest
import lit.fass.litfass.server.helper.KtorSupport
import org.junit.experimental.categories.Category
import spock.lang.Shared
import spock.lang.Specification

import static io.ktor.http.HttpMethod.Get
import static io.ktor.http.HttpStatusCode.OK
import static io.ktor.server.testing.TestEngineKt.handleRequest

/**
 * @author Michael Mair
 */
@Category(IntegrationTest)
class HealthRouteSpec extends Specification implements KtorSupport {

    @Shared
    def app

    def setupSpec() {
        app = initializeApp()
    }

    def "/health GET endpoint"() {
        when: "requesting /health"
        def result = handleRequest(app, Get, "/health", {}).response
        def resultContent = new JsonSlurper().parse(result.byteContent)

        then: "health status in JSON is returned"
        result.status() == OK
        resultContent.status == "OK"
    }
}
