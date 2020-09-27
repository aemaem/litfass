package lit.fass.server

import com.github.kittinunf.fuel.Fuel
import lit.fass.server.helper.End2EndSupport
import lit.fass.server.helper.TestTypes.End2EndTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test

/**
 * @author Michael Mair
 */
@Tag(End2EndTest)
internal class ApiTest : End2EndSupport() {

    @Test
    fun `actuator health GET endpoint`() {
        Fuel.get("${baseUrl()}/health").response { _, response, _ ->
            assertThat(response.statusCode).isEqualTo(200)
        }
    }
}