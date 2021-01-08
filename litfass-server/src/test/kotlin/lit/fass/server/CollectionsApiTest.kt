package lit.fass.server

import com.github.kittinunf.fuel.core.extensions.authentication
import com.github.kittinunf.fuel.httpGet
import com.github.kittinunf.fuel.httpPost
import com.github.kittinunf.fuel.jackson.jacksonDeserializerOf
import com.github.kittinunf.fuel.jackson.objectBody
import lit.fass.server.helper.TestTypes.ApiTest
import lit.fass.server.helper.TestcontainerSupport
import lit.fass.server.persistence.CollectionConfigPersistenceService.Companion.COLLECTION_CONFIG_TABLE
import org.assertj.core.api.Assertions.assertThat
import org.awaitility.Awaitility.with
import org.junit.jupiter.api.*
import org.junit.jupiter.api.MethodOrderer.OrderAnnotation
import org.junit.jupiter.api.TestInstance.Lifecycle.PER_CLASS
import org.springframework.core.io.ClassPathResource
import java.util.concurrent.TimeUnit.SECONDS

/**
 * @author Michael Mair
 */
@Tag(ApiTest)
@TestInstance(PER_CLASS)
@TestMethodOrder(OrderAnnotation::class)
internal class CollectionsApiTest : TestcontainerSupport() {

    @AfterAll
    fun cleanupTests() {
        dropTable("bar")
        dropTable("foo")
        clearTable(COLLECTION_CONFIG_TABLE)
    }

    @Test
    @Order(1)
    fun `collections {collection} GET endpoint`() {
        "/configs"
            .httpPost()
            .authentication().basic("admin", "admin")
            .body(ClassPathResource("bar.yml").file)
            .response()
            .apply {
                val response = second
                assertThat(response.statusCode).isEqualTo(204)
            }
        "/collections/bar?foo=bar&param1=foo&param1=bar&param2=true"
            .httpGet()
            .response()
            .apply {
                val response = second
                assertThat(response.statusCode).isEqualTo(200)
            }
        assertThat(litfassServer.logs).contains("Saved collection bar")
        with().pollDelay(3, SECONDS).await().until { selectAllFromTable("bar").size == 2 }
    }

    @Test
    @Order(2)
    fun `collections {collection} id GET endpoint`() {
        "/collections/bar/1"
            .httpGet()
            .authentication().basic("admin", "admin")
            .responseObject<Map<String, Any?>>(jacksonDeserializerOf())
            .apply {
                val result = third.component1()!!
                assertThat(result["id"]).isEqualTo("1")
                assertThat(result["bar"]).isEqualTo(true)
                assertThat((result["foo"] as Map<*, *>)["foo"]).isEqualTo("bar")
                assertThat((result["foo"] as Map<*, *>)["param1"]).isEqualTo("bar")
                assertThat((result["foo"] as Map<*, *>)["param2"]).isEqualTo("true")
                assertThat((result["foo"] as Map<*, *>)["timestamp"]).isNotNull
            }
        assertThat(litfassServer.logs).contains("Getting collection data for bar with id 1 for user admin")
    }

    @Test
    @Order(3)
    fun `collections {collection} id GET endpoint for second entry`() {
        "/collections/bar/2"
            .httpGet()
            .authentication().basic("admin", "admin")
            .responseObject<Map<String, Any?>>(jacksonDeserializerOf())
            .apply {
                val result = third.component1()!!
                assertThat(result["id"]).isEqualTo("2")
                assertThat(result["bar"]).isEqualTo(false)
                assertThat((result["foo"] as Map<*, *>)["blub"]).isEqualTo("servus")
            }
        assertThat(litfassServer.logs).contains("Getting collection data for bar with id 2 for user admin")
    }

    @Test
    @Order(4)
    fun `collections {collection} POST endpoint`() {
        "/configs"
            .httpPost()
            .authentication().basic("admin", "admin")
            .body(ClassPathResource("foo.yml").file)
            .response()
            .apply {
                val response = second
                assertThat(response.statusCode).isEqualTo(204)
            }
        "/collections/foo?param1=foo&param1=bar&param2=true"
            .httpPost()
            .objectBody(mapOf("id" to "1", "foo" to "bar"))
            .response()
            .apply {
                val response = second
                assertThat(response.statusCode).isEqualTo(200)
            }
        assertThat(litfassServer.logs).contains("Saved collection foo")
        with().pollDelay(3, SECONDS).await().until { selectAllFromTable("foo").size == 1 }
    }

    @Test
    @Order(5)
    fun `collections {collection} POST endpoint removes collection item`() {
        "/collections/foo?action=delete"
            .httpPost()
            .objectBody(mapOf("id" to "1"))
            .response()
            .apply {
                val response = second
                assertThat(response.statusCode).isEqualTo(200)
            }
        assertThat(litfassServer.logs).contains("Removed collection foo")
        with().pollDelay(3, SECONDS).await().until { selectAllFromTable("foo").size == 0 }
    }

}