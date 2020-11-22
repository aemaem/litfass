package lit.fass.server

import com.github.kittinunf.fuel.core.extensions.authentication
import com.github.kittinunf.fuel.httpPost
import com.github.kittinunf.fuel.jackson.jacksonDeserializerOf
import com.github.kittinunf.fuel.jackson.objectBody
import lit.fass.server.helper.TestTypes.ApiTest
import lit.fass.server.helper.TestcontainerSupport
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test

/**
 * @author Michael Mair
 */
@Tag(ApiTest)
internal class ScriptApiTest : TestcontainerSupport() {

    @Test
    fun `script extension test POST endpoint returns result`() {
        "/script/groovy/test"
            .httpPost()
            .authentication().basic("admin", "admin")
            .objectBody(
                mapOf(
                    "script" to """binding.data""",
                    "data" to mapOf("foo" to "bar", "bar" to true)
                )
            )
            .responseObject<List<Map<String, Any?>>>(jacksonDeserializerOf())
            .apply {
                val response = second
                val result = third.component1()!!
                assertThat(result).hasSize(1)
                assertThat(result.first()["foo"]).isEqualTo("bar")
                assertThat(result.first()["bar"]).isEqualTo(true)
                assertThat(response.statusCode).isEqualTo(200)
            }
    }
}