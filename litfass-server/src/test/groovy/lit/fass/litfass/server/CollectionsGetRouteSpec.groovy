package lit.fass.litfass.server

import lit.fass.litfass.server.config.ConfigService
import lit.fass.litfass.server.helper.IntegrationTest
import lit.fass.litfass.server.helper.LogCapture
import lit.fass.litfass.server.helper.PostgresSupport
import org.junit.Rule
import org.junit.experimental.categories.Category
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.web.server.LocalServerPort
import org.springframework.core.io.ClassPathResource
import org.springframework.test.context.ActiveProfiles
import spock.lang.Specification
import spock.lang.Stepwise

import static java.util.concurrent.TimeUnit.SECONDS
import static lit.fass.config.Profiles.POSTGRES
import static lit.fass.config.Profiles.TEST
import static org.awaitility.Awaitility.await
import static org.awaitility.Awaitility.with
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT
import static org.springframework.web.reactive.function.client.ExchangeFilterFunctions.basicAuthentication
import static org.springframework.web.reactive.function.client.WebClient.builder

/**
 * @author Michael Mair
 */
@Category(IntegrationTest)
@SpringBootTest(classes = ServerApplication, webEnvironment = RANDOM_PORT)
@ActiveProfiles([TEST, POSTGRES])
@Stepwise
class CollectionsGetRouteSpec extends Specification implements PostgresSupport {

    @LocalServerPort
    int port

    @Rule
    LogCapture log = new LogCapture()

    @Autowired
    ConfigService configService

    def setupSpec() {
        dropTable("bar")
    }

    def setup() {
        configService.readRecursively(new ClassPathResource("bar.yml").getFile())
    }


    def "/collections/{collection} GET endpoint"() {
        when: "requesting /collections/bar?param1=foo&param1=bar&param2=true"
        def result = builder().baseUrl("http://localhost:${port}/collections/bar?foo=bar&param1=foo&param1=bar&param2=true")
                .build()
                .get()
                .exchange()
                .block()
        then: "status is OK"
        result.statusCode().is2xxSuccessful()
        await().until { log.toString().contains("Saved collection bar") }
        and: "data is stored in database"
        with().pollDelay(3, SECONDS).await().until { selectAllFromTable("bar").size() == 2 }
    }

    def "/collections/{collection}/{id} GET endpoint"() {
        when: "requesting /collections/bar/1"
        def result = builder().baseUrl("http://localhost:${port}/collections/bar/1")
                .filter(basicAuthentication("admin", "admin"))
                .build()
                .get()
                .exchange()
                .block()
        def resultBody = result.bodyToMono(Map).block()
        then: "status is OK"
        result.statusCode().is2xxSuccessful()
        await().until { log.toString().contains("Getting collection data for bar with id 1 for user admin") }
        resultBody.id == "1"
        resultBody.bar == true
        resultBody.foo.foo == "bar"
        resultBody.foo.param1 == "foo,bar"
        resultBody.foo.param2 == "true"
        resultBody.foo.timestamp
    }

    def "/collections/{collection}/{id} GET endpoint for second entry"() {
        when: "requesting /collections/bar/2"
        def result = builder().baseUrl("http://localhost:${port}/collections/bar/2")
                .filter(basicAuthentication("admin", "admin"))
                .build()
                .get()
                .exchange()
                .block()
        def resultBody = result.bodyToMono(Map).block()
        then: "status is OK"
        result.statusCode().is2xxSuccessful()
        await().until { log.toString().contains("Getting collection data for bar with id 2 for user admin") }
        resultBody.id == "2"
        resultBody.bar == false
        resultBody.foo.blub == "servus"
    }
}
