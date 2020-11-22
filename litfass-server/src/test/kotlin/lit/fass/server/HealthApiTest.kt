package lit.fass.server

import com.github.kittinunf.fuel.httpGet
import lit.fass.server.helper.TestTypes.ApiTest
import lit.fass.server.helper.TestcontainerSupport
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test

/**
 * @author Michael Mair
 */
@Tag(ApiTest)
internal class ApiTest : TestcontainerSupport() {

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