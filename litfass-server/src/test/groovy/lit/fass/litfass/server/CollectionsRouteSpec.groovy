package lit.fass.litfass.server

import lit.fass.litfass.server.helper.ElasticsearchSupport
import lit.fass.litfass.server.helper.IntegrationTest
import lit.fass.litfass.server.helper.KtorSupport
import lit.fass.litfass.server.helper.LogCapture
import org.junit.Rule
import org.junit.experimental.categories.Category
import spock.lang.Shared
import spock.lang.Specification

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
class CollectionsRouteSpec extends Specification implements KtorSupport, ElasticsearchSupport {

    @Shared
    def app

    @Rule
    LogCapture log = new LogCapture()

    def setupSpec() {
        app = initializeApp([
                "litfass.config.collection.path": this.class.getResource("/foo.yml").file
        ])
    }

    def setup() {
        cleanDatabase()
    }

    def "/collections/{collection} POST endpoint"() {
        when: "requesting /collections/foo?param1=foo&param1=bar&param2=true"
        def result = handleRequest(app, Post, "/collections/foo?param1=foo&param1=bar&param2=true", {
            withBody(["foo": "bar"], it)
        }).response

        then: "status is OK"
        result.status() == OK
        result.content == null
        await().until { log.toString().contains("Indexed record") }
        and: "data is stored in database"
        with().pollDelay(3, SECONDS).await().until { findAllIndex("foo").hits.totalHits == 1 }
    }
}
