package lit.fass.litfass.server


import groovy.json.JsonSlurper
import lit.fass.litfass.server.helper.IntegrationTest
import lit.fass.litfass.server.helper.KtorSupport
import org.junit.experimental.categories.Category
import spock.lang.Shared
import spock.lang.Specification

import static io.ktor.http.HttpMethod.Get
import static io.ktor.http.HttpStatusCode.OK
import static io.ktor.http.HttpStatusCode.Unauthorized
import static io.ktor.server.testing.TestEngineKt.handleRequest

/**
 * @author Michael Mair
 */
@Category(IntegrationTest)
class RootRouteSpec extends Specification implements KtorSupport {

    @Shared
    def app

    def setupSpec() {
        app = initializeApp()
    }

    def "/ GET endpoint"() {
        when: "requesting /"
        def result = handleRequest(app, Get, "/", { withBasicAuth("admin", "admin", it) }).response
        def resultContent = new JsonSlurper().parse(result.byteContent)

        then: "health status in JSON is returned"
        result.status() == OK
        resultContent.application == "LITFASS"
        resultContent.description == "Lightweight Integrated Tailorable Flow Aware Software Service"
    }

    def "/ GET endpoint is secured"() {
        when: "requesting / unauthorized"
        def result = handleRequest(app, Get, "/", {}).response

        then: "access is forbidden"
        result.status() == Unauthorized
        result.content == null
    }
}
