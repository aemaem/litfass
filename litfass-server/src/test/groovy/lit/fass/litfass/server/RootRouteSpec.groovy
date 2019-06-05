package lit.fass.litfass.server

import groovy.json.JsonSlurper
import lit.fass.litfass.server.helper.IntegrationTest
import org.junit.experimental.categories.Category
import spock.lang.Ignore
import spock.lang.Shared
import spock.lang.Specification


/**
 * @author Michael Mair
 */
@Ignore
@Category(IntegrationTest)
class RootRouteSpec extends Specification {

    @Shared
    def app

    def setupSpec() {
        app = initializeApp([
                testing                : true,
                "litfass.jdbc.poolSize": 2
        ])
    }

    def cleanupSpec() {
        stopApp(app)
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
