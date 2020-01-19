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
import static org.springframework.http.HttpStatus.UNAUTHORIZED
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
class ScriptRouteSpec extends Specification {

    @LocalServerPort
    int port

    def "/script/{extension}/test POST endpoint is secured"() {
        when: "requesting /script/{language}/test unauthorized"
        def result = builder().baseUrl("http://localhost:${port}/script/kotlin/test")
                .build()
                .post()
                .exchange()
                .block()
        then: "access is forbidden"
        result.statusCode() == UNAUTHORIZED
    }

    def "/script/{extension}/test POST endpoint returns result"() {
        when: "requesting /script/{language}/test"
        def result = builder().baseUrl("http://localhost:${port}/script/kotlin/test")
                .filter(basicAuthentication("admin", "admin"))
                .build()
                .post()
                .contentType(APPLICATION_JSON_UTF8)
                .body(fromObject([
                        script: """bindings["data"]""",
                        data  : [foo: "bar", bar: true]
                ]))
                .exchange()
                .block()
        def resultContent = result.bodyToMono(List).block()

        then: "result is returned"
        result.statusCode().is2xxSuccessful()
        resultContent.size() == 1
        resultContent.first().foo == "bar"
        resultContent.first().bar == true
    }
}
