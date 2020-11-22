package lit.fass.server

import com.github.kittinunf.fuel.Fuel
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
        Fuel.get("${baseUrl()}/health").response { _, response, _ ->
            assertThat(response.statusCode).isEqualTo(200)
        }
    }
}