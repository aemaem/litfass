package lit.fass.server.http

import lit.fass.server.helper.TestTypes.UnitTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test

/**
 * @author Michael Mair
 */
@Tag(UnitTest)
internal class CollectionHttpServiceTest {

    @Test
    fun `simple get`() {
        val result = CollectionHttpService()
            .get(
                "https://circleci.com/api/v1.1/project/github/leftshiftone/keios-protocol/125",
                emptyMap(),
                "553f9c18552c7314bdfdc7740b7febe05e0ea5a9",
                ""
            )
        assertThat(result).isNotEmpty
    }

    @Test
    fun `simple get with headers`() {
        val result = CollectionHttpService()
            .get(
                "https://circleci.com/api/v1.1/project/github/leftshiftone/keios-protocol/125",
                mapOf("X-foo" to "bar"),
                "553f9c18552c7314bdfdc7740b7febe05e0ea5a9",
                ""
            )
        assertThat(result["requestHeaders"]).isEqualTo(mapOf("X-foo" to "bar"))
    }

    @Test
    fun `response can be parsed for map`() {
        val response = """{"foo": "bar","email": "foo@bar"}""".toByteArray()
        val result = CollectionHttpService().parse(response)
        assertThat(result["foo"]).isEqualTo("bar")
        assertThat(result["email"]).isEqualTo("foo@bar")
    }

    @Test
    fun `response can be parsed for list`() {
        val response = """[{"foo": "bar","email": "foo@bar"},{"bar": "foo","email": "bar@foo"}]""".toByteArray()
        val result = CollectionHttpService().parse(response)

        @Suppress("UNCHECKED_CAST")
        val httpResult = result["http"] as List<Map<*, *>>
        assertThat(httpResult[0]).extracting("foo").isEqualTo("bar")
        assertThat(httpResult[0]).extracting("email").isEqualTo("foo@bar")
        assertThat(httpResult[1]).extracting("bar").isEqualTo("foo")
        assertThat(httpResult[1]).extracting("email").isEqualTo("bar@foo")
    }
}