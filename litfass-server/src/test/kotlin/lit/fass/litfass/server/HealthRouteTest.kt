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
import org.springframework.test.context.ActiveProfiles
import org.springframework.web.reactive.function.client.WebClient
import reactor.kotlin.test.test

/**
 * @author Michael Mair
 */
@Tag(IntegrationTest)
@SpringBootTest(classes = [ServerApplication::class], webEnvironment = RANDOM_PORT)
@ActiveProfiles(TEST, POSTGRES)
internal class HealthRouteTest {

    @LocalServerPort
    var port: Int? = null

    @Test
    fun `actuator health GET endpoint`() {
        WebClient.builder()
            .baseUrl("http://localhost:${port}/actuator/health")
            .build()
            .get()
            .exchange()
            .flatMap {
                it.bodyToMono(Map::class.java)
            }
            .test()
            .assertNext {
                assertThat(it["status"]).isEqualTo("UP")
            }
            .expectComplete()
            .verify()
    }
}