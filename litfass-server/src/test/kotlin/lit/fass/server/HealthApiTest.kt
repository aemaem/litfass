package lit.fass.server

import com.github.kittinunf.fuel.httpGet
import lit.fass.server.helper.TestTypes.ApiTest
import lit.fass.server.helper.TestcontainerSupport
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.TestInstance.Lifecycle.PER_CLASS

/**
 * @author Michael Mair
 */
@Tag(ApiTest)
@TestInstance(PER_CLASS)
internal class HealthApiTest : TestcontainerSupport() {

    @Test
    fun `actuator ready GET endpoint`() {
        "/ready"
            .httpGet()
            .response()
            .apply {
                val response = second
                assertThat(response.statusCode).isEqualTo(200)
            }
    }

    @Test
    fun `actuator health GET endpoint`() {
        "/health"
            .httpGet()
            .response()
            .apply {
                val response = second
                assertThat(response.statusCode).isEqualTo(200)
            }
    }

}