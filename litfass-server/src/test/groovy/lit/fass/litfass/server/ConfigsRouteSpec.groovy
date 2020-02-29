package lit.fass.litfass.server


import lit.fass.litfass.server.helper.IntegrationTest
import lit.fass.litfass.server.helper.LogCapture
import org.junit.Rule
import org.junit.experimental.categories.Category
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.web.server.LocalServerPort
import org.springframework.test.context.ActiveProfiles
import spock.lang.Specification
import spock.lang.Stepwise

import static lit.fass.config.Profiles.POSTGRES
import static lit.fass.config.Profiles.TEST
import static org.awaitility.Awaitility.await
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT
import static org.springframework.http.HttpStatus.UNAUTHORIZED
import static org.springframework.http.MediaType.TEXT_PLAIN
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
class ConfigsRouteSpec extends Specification {

    @LocalServerPort
    int port

    @Rule
    LogCapture log = new LogCapture()

    def "/configs POST endpoint is secured"() {
        when: "requesting /configs unauthorized"
        def result = builder().baseUrl("http://localhost:${port}/configs")
                .build()
                .get()
                .exchange()
                .block()
        then: "access is forbidden"
        result.statusCode() == UNAUTHORIZED
    }

    def "/configs POST endpoint"() {
        when: "requesting /configs"
        def result1 = builder().baseUrl("http://localhost:${port}/configs")
                .filter(basicAuthentication("admin", "admin"))
                .build()
                .post()
                .contentType(TEXT_PLAIN)
                .body(fromObject("""
                collection: foo
                flows:
                  - flow:
                      steps:
                        - script:
                            description: "Transform something"
                            language: kotlin
                            code: println("bar")
                """.stripIndent()))
                .exchange()
                .block()
        and:
        def result2 = builder().baseUrl("http://localhost:${port}/configs")
                .filter(basicAuthentication("admin", "admin"))
                .build()
                .post()
                .contentType(TEXT_PLAIN)
                .body(fromObject("""
                collection: bar
                flows:
                  - flow:
                      steps:
                        - script:
                            description: "Transform something"
                            language: kotlin
                            code: println("bar")
                """.stripIndent()))
                .exchange()
                .block()
        then: "configs are created"
        result1.statusCode().is2xxSuccessful()
        result2.statusCode().is2xxSuccessful()
    }

    def "/configs POST endpoint schedules config"() {
        when: "requesting /configs"
        def result = builder().baseUrl("http://localhost:${port}/configs")
                .filter(basicAuthentication("admin", "admin"))
                .build()
                .post()
                .contentType(TEXT_PLAIN)
                .body(fromObject("""
                collection: foo
                scheduled: "* * * * * ? *"
                retention: "P7D"
                flows:
                  - flow:
                      steps:
                        - script:
                            description: "Transform something"
                            language: KOTLIN
                            code: println("bar")
                """.stripIndent()))
                .exchange()
                .block()
        then: "configs are created and scheduled"
        await().until {
            log.toString().contains("Creating scheduled collection job foo with cron * * * * * ? *")
            log.toString().contains("Collection job foo to be scheduled every second")
            log.toString().contains("Creating scheduled retention job foo with cron 0 0 0 ? * SUN *")
            log.toString().contains("Retention job foo to be scheduled at 00:00 at Sunday day")
        }
        result.statusCode().is2xxSuccessful()
    }

    def "/configs GET endpoint is secured"() {
        when: "requesting /configs unauthorized"
        def result = builder().baseUrl("http://localhost:${port}/configs")
                .build()
                .get()
                .exchange()
                .block()
        then: "access is forbidden"
        result.statusCode() == UNAUTHORIZED
    }

    def "/configs GET endpoint"() {
        when: "requesting /configs"
        def result = builder().baseUrl("http://localhost:${port}/configs")
                .filter(basicAuthentication("admin", "admin"))
                .build()
                .get()
                .exchange()
                .block()
        def resultContent = result.bodyToMono(Collection).block()
        then: "configs are returned"
        result.statusCode().is2xxSuccessful()
        resultContent.size() == 2
        resultContent[0].collection == "foo"
        resultContent[0].flows.size() == 1
        resultContent[1].collection == "bar"
        resultContent[1].flows.size() == 1
    }

    def "/configs/{collection} GET endpoint"() {
        when: "requesting /configs/{collection}"
        def result = builder().baseUrl("http://localhost:${port}/configs/foo")
                .filter(basicAuthentication("admin", "admin"))
                .build()
                .get()
                .exchange()
                .block()
        def resultContent = result.bodyToMono(Map).block()
        then: "config is returned"
        result.statusCode().is2xxSuccessful()
        resultContent.collection == "foo"
        resultContent.flows.size() == 1
    }

    def "/configs/{collection} DELETE endpoint"() {
        when: "requesting /configs/{collection}"
        def result1 = builder().baseUrl("http://localhost:${port}/configs/foo")
                .filter(basicAuthentication("admin", "admin"))
                .build()
                .delete()
                .exchange()
                .block()
        and:
        def result2 = builder().baseUrl("http://localhost:${port}/configs/bar")
                .filter(basicAuthentication("admin", "admin"))
                .build()
                .delete()
                .exchange()
                .block()
        then: "response is successful"
        result1.statusCode().is2xxSuccessful()
        result2.statusCode().is2xxSuccessful()
    }
}
