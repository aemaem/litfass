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
class ScriptRouteSpec extends Specification {

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

    def "/script/{extension}/test POST endpoint is secured"() {
        when: "requesting /script/{language}/test unauthorized"
        def result = handleRequest(app, Post, "/script/kotlin/test", {}).response

        then: "access is forbidden"
        result.status() == Unauthorized
        result.content == null
    }

    def "/script/{extension}/test POST endpoint returns result"() {
        when: "requesting /script/{language}/test"
        def result = handleRequest(app, Post, "/script/kotlin/test", {
            withBasicAuth("admin", "admin", it)
            withBody([
                    script: """bindings["data"]""",
                    data  : [foo: "bar", bar: true]
            ], it)
        }).response
        def resultContent = new JsonSlurper().parse(result.byteContent)

        then: "result is returned"
        result.status() == OK
        resultContent.foo == "bar"
        resultContent.bar == true
    }
}
