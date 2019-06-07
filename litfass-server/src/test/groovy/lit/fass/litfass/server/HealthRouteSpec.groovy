package lit.fass.litfass.server


import lit.fass.litfass.server.helper.IntegrationTest
import org.junit.experimental.categories.Category
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.web.server.LocalServerPort
import org.springframework.test.context.ActiveProfiles
import spock.lang.Specification

import static lit.fass.config.Profiles.POSTGRES
import static lit.fass.config.Profiles.TEST
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT
import static org.springframework.web.reactive.function.client.WebClient.builder

/**
 * @author Michael Mair
 */
@Category(IntegrationTest)
@SpringBootTest(classes = ServerApplication, webEnvironment = RANDOM_PORT)
@ActiveProfiles([TEST, POSTGRES])
class HealthRouteSpec extends Specification {

    @LocalServerPort
    int port

    def "/actuator/health GET endpoint"() {
        when: "requesting /actuator/health"
        def result = builder().baseUrl("http://localhost:${port}/actuator/health")
                .build()
                .get()
                .exchange()
                .block()
        def resultContent = result.bodyToMono(Map).block()

        then: "health status in JSON is returned"
        result.statusCode().is2xxSuccessful()
        resultContent.status == "UP"
    }
}
