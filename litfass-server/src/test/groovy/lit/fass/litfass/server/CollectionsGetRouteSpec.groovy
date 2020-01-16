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
import static org.springframework.http.MediaType.APPLICATION_JSON_UTF8
import static org.springframework.web.reactive.function.BodyInserters.fromObject
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
        dropTable("foo")
    }

    def setup() {
        configService.readRecursively(new ClassPathResource("foo.yml").getFile())
    }


    def "/collections/{collection} GET endpoint"() {
        when: "requesting /collections/foo?param1=foo&param1=bar&param2=true"
        def result = builder().baseUrl("http://localhost:${port}/collections/foo?id=1&foo=bar&param1=foo&param1=bar&param2=true")
                .build()
                .get()
                .exchange()
                .block()
        then: "status is OK"
        result.statusCode().is2xxSuccessful()
        await().until { log.toString().contains("Saved collection foo") }
        and: "data is stored in database"
        with().pollDelay(3, SECONDS).await().until { selectAllFromTable("foo").size() == 1 }
    }

    def "/collections/{collection}/{id} GET endpoint"() {
        when: "requesting /collections/foo/1"
        def result = builder().baseUrl("http://localhost:${port}/collections/foo/1")
                .filter(basicAuthentication("admin", "admin"))
                .build()
                .get()
                .exchange()
                .block()
        def resultBody = result.bodyToMono(Map).block()
        then: "status is OK"
        result.statusCode().is2xxSuccessful()
        await().until { log.toString().contains("Getting collection data for foo with id 1 for user admin") }
        resultBody.id == "1"
        resultBody.foo == "bar"
        resultBody.param1 == "foo,bar"
        resultBody.param2 == "true"
        resultBody.timestamp
    }
}
