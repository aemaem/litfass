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
class HealthRouteSpec extends Specification  {

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

    def "/health GET endpoint"() {
        when: "requesting /health"
        def result = handleRequest(app, Get, "/health", {}).response
        def resultContent = new JsonSlurper().parse(result.byteContent)

        then: "health status in JSON is returned"
        result.status() == OK
        resultContent.status == "OK"
    }
}
