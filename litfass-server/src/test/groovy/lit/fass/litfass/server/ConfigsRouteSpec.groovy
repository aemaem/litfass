package lit.fass.litfass.server

import groovy.json.JsonSlurper
import lit.fass.litfass.server.helper.IntegrationTest
import lit.fass.litfass.server.helper.KtorSupport
import org.junit.experimental.categories.Category
import spock.lang.Shared
import spock.lang.Specification
import spock.lang.Stepwise

import static io.ktor.http.HttpMethod.*
import static io.ktor.http.HttpStatusCode.*
import static io.ktor.server.testing.TestEngineKt.handleRequest

/**
 * @author Michael Mair
 */
@Category(IntegrationTest)
@Stepwise
class ConfigsRouteSpec extends Specification implements KtorSupport {

    @Shared
    def app

    def setupSpec() {
        app = initializeApp()
    }

    def "/configs POST endpoint is secured"() {
        when: "requesting /configs unauthorized"
        def result = handleRequest(app, Post, "/configs", {}).response

        then: "access is forbidden"
        result.status() == Unauthorized
        result.content == null
    }

    def "/configs POST endpoint"() {
        when: "requesting /configs"
        def result = handleRequest(app, Post, "/configs", {
            withBody("""
collection: foo
flow:
  - script:
      description: "Transform something"
      language: kts
      code: bindings["data"]
            """, it)
            withBasicAuth("admin", "admin", it)
        }).response

        then: "configs are returned"
        result.status() == OK
    }

    def "/configs GET endpoint is secured"() {
        when: "requesting /configs unauthorized"
        def result = handleRequest(app, Get, "/configs", {}).response

        then: "access is forbidden"
        result.status() == Unauthorized
        result.content == null
    }

    def "/configs GET endpoint"() {
        when: "requesting /configs"
        def result = handleRequest(app, Get, "/configs", { withBasicAuth("admin", "admin", it) }).response
        def resultContent = new JsonSlurper().parse(result.byteContent)

        then: "configs are returned"
        result.status() == OK
        resultContent.size() == 1
        resultContent[0].collection == "foo"
        resultContent[0].flow.size() == 1
    }

    def "/configs/{collection} GET endpoint"() {
        when: "requesting /configs/{collection}"
        def result = handleRequest(app, Get, "/configs/foo", { withBasicAuth("admin", "admin", it) }).response
        def resultContent = new JsonSlurper().parse(result.byteContent)

        then: "configs are returned"
        result.status() == OK
        resultContent.collection == "foo"
        resultContent.flow.size() == 1
    }

    def "/configs/{collection} DELETE endpoint"() {
        when: "requesting /configs/{collection}"
        def result = handleRequest(app, Delete, "/configs/foo", { withBasicAuth("admin", "admin", it) }).response

        then: "configs are returned"
        result.status() == NoContent
    }
}
