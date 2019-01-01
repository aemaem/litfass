package lit.fass.litfass.server

import groovy.json.JsonSlurper
import lit.fass.litfass.server.helper.IntegrationTest
import lit.fass.litfass.server.helper.KtorSupport
import lit.fass.litfass.server.helper.LogCapture
import lit.fass.litfass.server.helper.PostgresSupport
import org.junit.Rule
import org.junit.experimental.categories.Category
import spock.lang.Shared
import spock.lang.Specification
import spock.lang.Stepwise

import static io.ktor.http.HttpMethod.Get
import static io.ktor.http.HttpMethod.Post
import static io.ktor.http.HttpStatusCode.OK
import static io.ktor.server.testing.TestEngineKt.handleRequest
import static java.util.concurrent.TimeUnit.SECONDS
import static org.awaitility.Awaitility.await
import static org.awaitility.Awaitility.with

/**
 * @author Michael Mair
 */
@Category(IntegrationTest)
@Stepwise
class CollectionsRouteSpec extends Specification implements KtorSupport, PostgresSupport {

    @Shared
    def app

    @Rule
    LogCapture log = new LogCapture()

    def setupSpec() {
        app = initializeApp([
                "testing"                       : true,
                "litfass.jdbc.poolSize"         : 2,
                "litfass.config.collection.path": this.class.getResource("/foo.yml").file
        ])
        dropTable("foo")
    }

    def cleanupSpec() {
        stopApp(app)
    }

    def "/collections/{collection} POST endpoint"() {
        when: "requesting /collections/foo?param1=foo&param1=bar&param2=true"
        def result = handleRequest(app, Post, "/collections/foo?param1=foo&param1=bar&param2=true", {
            withBody(["id": "1", "foo": "bar"], it)
        }).response

        then: "status is OK"
        result.status() == OK
        result.content == null
        await().until { log.toString().contains("Saved collection foo") }
        and: "data is stored in database"
        with().pollDelay(3, SECONDS).await().until { selectAllFromTable("foo").size() == 1 }
    }

    def "/collections/{collection}/{id} GET endpoint"() {
        when: "requesting /collections/foo/1"
        def result = handleRequest(app, Get, "/collections/foo/1", { withBasicAuth("admin", "admin", it) }).response
        def resultBody = new JsonSlurper().parseText(result.content)

        then: "status is OK"
        result.status() == OK
        resultBody.id == "1"
        resultBody.foo == "bar"
        resultBody.param1 == "foo,bar"
        resultBody.param2 == "true"
        resultBody.timestamp
    }
}
