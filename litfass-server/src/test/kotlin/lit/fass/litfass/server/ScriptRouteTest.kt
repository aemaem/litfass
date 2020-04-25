package lit.fass.litfass.server

import lit.fass.config.Profiles.Companion.POSTGRES
import lit.fass.config.Profiles.Companion.TEST
import lit.fass.litfass.server.helper.IntegrationTest.IntegrationTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT
import org.springframework.boot.web.server.LocalServerPort
import org.springframework.core.ParameterizedTypeReference
import org.springframework.http.HttpStatus.UNAUTHORIZED
import org.springframework.http.MediaType.APPLICATION_JSON
import org.springframework.test.context.ActiveProfiles
import org.springframework.web.reactive.function.BodyInserters.fromValue
import org.springframework.web.reactive.function.client.ExchangeFilterFunctions.basicAuthentication
import org.springframework.web.reactive.function.client.WebClient
import reactor.kotlin.test.test

/**
 * @author Michael Mair
 */
@Tag(IntegrationTest)
@SpringBootTest(classes = [ServerApplication::class], webEnvironment = RANDOM_PORT)
@ActiveProfiles(TEST, POSTGRES)
internal class ScriptRouteTest {

    @LocalServerPort
    var port: Int? = null

    @Test
    fun `script extension test POST endpoint is secured`() {
        WebClient.builder()
            .baseUrl("http://localhost:${port}/script/groovy/test")
            .build()
            .get()
            .exchange()
            .test()
            .assertNext {
                assertThat(it.statusCode()).isEqualTo(UNAUTHORIZED)
            }
            .expectComplete()
            .verify()
    }

    @Test
    fun `script extension test POST endpoint returns result`() {
        WebClient.builder()
            .baseUrl("http://localhost:${port}/script/groovy/test")
            .filter(basicAuthentication("admin", "admin"))
            .build()
            .post()
            .contentType(APPLICATION_JSON)
            .body(
                fromValue(
                    mapOf(
                        "script" to """binding.data""",
                        "data" to mapOf("foo" to "bar", "bar" to true)
                    )
                )
            )
            .exchange()
            .flatMap { response ->
                response.bodyToMono(object : ParameterizedTypeReference<List<Map<String, Any?>>>() {})
            }
            .test()
            .assertNext { result ->
                assertThat(result).hasSize(1)
                assertThat(result.first()["foo"]).isEqualTo("bar")
                assertThat(result.first()["bar"]).isEqualTo(true)
            }
            .expectComplete()
            .verify()
    }
}