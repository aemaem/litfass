package lit.fass.litfass.server

import lit.fass.config.Profiles.Companion.POSTGRES
import lit.fass.config.Profiles.Companion.TEST
import lit.fass.litfass.server.helper.IntegrationTest.IntegrationTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.*
import org.junit.jupiter.api.MethodOrderer.OrderAnnotation
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT
import org.springframework.boot.test.system.CapturedOutput
import org.springframework.boot.test.system.OutputCaptureExtension
import org.springframework.boot.web.server.LocalServerPort
import org.springframework.core.ParameterizedTypeReference
import org.springframework.http.HttpStatus.UNAUTHORIZED
import org.springframework.http.MediaType.TEXT_PLAIN
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
@TestMethodOrder(OrderAnnotation::class)
@ExtendWith(OutputCaptureExtension::class)
internal class ConfigsRouteTest {

    @LocalServerPort
    var port: Int? = null

    @Test
    fun `configs POST endpoint is secured`() {
        WebClient.builder()
            .baseUrl("http://localhost:${port}/configs")
            .build()
            .post()
            .exchange()
            .test()
            .assertNext {
                assertThat(it.statusCode()).isEqualTo(UNAUTHORIZED)
            }
            .expectComplete()
            .verify()
    }

    @Test
    @Order(1)
    fun `configs POST endpoint`() {
        WebClient.builder()
            .baseUrl("http://localhost:${port}/configs")
            .filter(basicAuthentication("admin", "admin"))
            .build()
            .post()
            .contentType(TEXT_PLAIN)
            .body(
                fromValue(
                    """
                collection: foo
                flows:
                  - flow:
                      steps:
                        - script:
                            description: "Transform something"
                            language: kotlin
                            code: println("bar") 
                """.trimIndent()
                )
            )
            .exchange()
            .test()
            .assertNext {
                assertThat(it.statusCode().is2xxSuccessful).isTrue()
            }
            .expectComplete()
            .verify()

        WebClient.builder()
            .baseUrl("http://localhost:${port}/configs")
            .filter(basicAuthentication("admin", "admin"))
            .build()
            .post()
            .contentType(TEXT_PLAIN)
            .body(
                fromValue(
                    """
                collection: bar
                flows:
                  - flow:
                      steps:
                        - script:
                            description: "Transform something"
                            language: groovy
                            code: println "bar"
                """.trimIndent()
                )
            )
            .exchange()
            .test()
            .assertNext {
                assertThat(it.statusCode().is2xxSuccessful).isTrue()
            }
            .expectComplete()
            .verify()
    }

    @Test
    @Order(2)
    fun `configs POST endpoint schedules config`(output: CapturedOutput) {
        WebClient.builder()
            .baseUrl("http://localhost:${port}/configs")
            .filter(basicAuthentication("admin", "admin"))
            .build()
            .post()
            .contentType(TEXT_PLAIN)
            .body(
                fromValue(
                    """
                collection: foo
                scheduled: "* * * * * ? *"
                retention: "P7D"
                flows:
                  - flow:
                      steps:
                        - script:
                            description: "Transform something"
                            language: GROOVY
                            code: println "bar"
                """.trimIndent()
                )
            )
            .exchange()
            .test()
            .assertNext {
                assertThat(it.statusCode().is2xxSuccessful).isTrue()
            }
            .expectComplete()
            .verify()

        assertThat(output).contains(
            "Creating scheduled collection job foo with cron * * * * * ? *",
            "Creating scheduled collection job foo with cron * * * * * ? *",
            "Collection job foo to be scheduled every second",
            "Creating scheduled retention job foo with cron 0 0 0 ? * SUN *",
            "Retention job foo to be scheduled at 00:00 at Sunday day"
        )
    }

    @Test
    fun `configs GET endpoint is secured`() {
        WebClient.builder()
            .baseUrl("http://localhost:${port}/configs")
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
    @Order(3)
    fun `configs GET endpoint`() {
        WebClient.builder()
            .baseUrl("http://localhost:${port}/configs")
            .filter(basicAuthentication("admin", "admin"))
            .build()
            .get()
            .exchange()
            .flatMap { response ->
                response.bodyToMono(object : ParameterizedTypeReference<List<Map<String, Any?>>>() {})
            }
            .test()
            .assertNext {
                assertThat(it).hasSize(2)
                assertThat(it[0]["collection"]).isEqualTo("foo")
                assertThat(it[0]["flows"]).isNotNull
                assertThat(it[1]["collection"]).isEqualTo("bar")
                assertThat(it[1]["flows"]).isNotNull
            }
            .expectComplete()
            .verify()
    }

    @Test
    @Order(4)
    fun `configs {collection} GET endpoint`() {
        WebClient.builder()
            .baseUrl("http://localhost:${port}/configs/foo")
            .filter(basicAuthentication("admin", "admin"))
            .build()
            .get()
            .exchange()
            .flatMap { response ->
                response.bodyToMono(object : ParameterizedTypeReference<Map<String, Any?>>() {})
            }
            .test()
            .assertNext {
                assertThat(it["collection"]).isEqualTo("foo")
                assertThat(it["flows"] is Collection<*>).isTrue()
                assertThat(it["flows"] as Collection<*>).hasSize(1)
            }
            .expectComplete()
            .verify()
    }

    @Test
    @Order(5)
    fun `configs {collection} DELETE endpoint`() {
        WebClient.builder()
            .baseUrl("http://localhost:${port}/configs/foo")
            .filter(basicAuthentication("admin", "admin"))
            .build()
            .delete()
            .exchange()
            .test()
            .assertNext {
                assertThat(it.statusCode().is2xxSuccessful).isTrue()
            }
            .expectComplete()
            .verify()

        WebClient.builder()
            .baseUrl("http://localhost:${port}/configs/bar")
            .filter(basicAuthentication("admin", "admin"))
            .build()
            .delete()
            .exchange()
            .test()
            .assertNext {
                assertThat(it.statusCode().is2xxSuccessful).isTrue()
            }
            .expectComplete()
            .verify()
    }
}