package lit.fass.litfass.server

import lit.fass.config.Profiles.Companion.POSTGRES
import lit.fass.config.Profiles.Companion.TEST
import lit.fass.litfass.server.config.ConfigService
import lit.fass.litfass.server.helper.IntegrationTest.IntegrationTest
import lit.fass.litfass.server.helper.PostgresSupport
import org.assertj.core.api.Assertions.assertThat
import org.awaitility.Awaitility.with
import org.junit.jupiter.api.*
import org.junit.jupiter.api.MethodOrderer.OrderAnnotation
import org.junit.jupiter.api.TestInstance.Lifecycle.PER_CLASS
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT
import org.springframework.boot.test.system.CapturedOutput
import org.springframework.boot.test.system.OutputCaptureExtension
import org.springframework.boot.web.server.LocalServerPort
import org.springframework.core.ParameterizedTypeReference
import org.springframework.core.io.ClassPathResource
import org.springframework.test.context.ActiveProfiles
import org.springframework.web.reactive.function.client.ExchangeFilterFunctions.basicAuthentication
import org.springframework.web.reactive.function.client.WebClient
import reactor.kotlin.test.test
import java.util.concurrent.TimeUnit.SECONDS

/**
 * @author Michael Mair
 */
@Tag(IntegrationTest)
@SpringBootTest(classes = [ServerApplication::class], webEnvironment = RANDOM_PORT)
@ActiveProfiles(TEST, POSTGRES)
@TestInstance(PER_CLASS)
@TestMethodOrder(OrderAnnotation::class)
@ExtendWith(OutputCaptureExtension::class)
internal class CollectionsGetRouteTest : PostgresSupport() {

    @LocalServerPort
    var port: Int? = null

    @Autowired
    var configService: ConfigService? = null

    @BeforeAll
    fun setupTest() {
        dropTable("bar")
    }

    @BeforeEach
    fun setup() {
        configService!!.readRecursively(ClassPathResource("bar.yml").file)
    }

    @Test
    @Order(1)
    fun `collections {collection} GET endpoint`(output: CapturedOutput) {
        WebClient.builder()
            .baseUrl("http://localhost:${port}/collections/bar?foo=bar&param1=foo&param1=bar&param2=true")
            .build()
            .get()
            .exchange()
            .test()
            .assertNext {
                assertThat(it.statusCode().is2xxSuccessful).isTrue()
            }
            .expectComplete()
            .verify()
        assertThat(output).contains("Saved collection bar")
        with().pollDelay(3, SECONDS).await().until { selectAllFromTable("bar").size == 2 }
    }

    @Test
    @Order(2)
    fun `collections {collection} id GET endpoint`(output: CapturedOutput) {
        WebClient.builder()
            .baseUrl("http://localhost:${port}/collections/bar/1")
            .filter(basicAuthentication("admin", "admin"))
            .build()
            .get()
            .exchange()
            .flatMap { response ->
                response.bodyToMono(object : ParameterizedTypeReference<Map<String, Any?>>() {})
            }
            .test()
            .assertNext {
                assertThat(it["id"]).isEqualTo("1")
                assertThat(it["bar"]).isEqualTo(true)
                assertThat((it["foo"] as Map<*, *>)["foo"]).isEqualTo("bar")
                assertThat((it["foo"] as Map<*, *>)["param1"]).isEqualTo("foo,bar")
                assertThat((it["foo"] as Map<*, *>)["param2"]).isEqualTo("true")
                assertThat((it["foo"] as Map<*, *>)["timestamp"]).isNotNull
            }
            .expectComplete()
            .verify()
        assertThat(output).contains("Getting collection data for bar with id 1 for user admin")
    }

    @Test
    @Order(3)
    fun `collections {collection} id GET endpoint for second entry`(output: CapturedOutput) {
        WebClient.builder()
            .baseUrl("http://localhost:${port}/collections/bar/2")
            .filter(basicAuthentication("admin", "admin"))
            .build()
            .get()
            .exchange()
            .flatMap { response ->
                response.bodyToMono(object : ParameterizedTypeReference<Map<String, Any?>>() {})
            }
            .test()
            .assertNext {
                assertThat(it["id"]).isEqualTo("2")
                assertThat(it["bar"]).isEqualTo(false)
                assertThat((it["foo"] as Map<*, *>)["blub"]).isEqualTo("servus")
            }
            .expectComplete()
            .verify()
        assertThat(output).contains("Getting collection data for bar with id 2 for user admin")
    }

}