package lit.fass.server

import com.github.kittinunf.fuel.core.extensions.authentication
import com.github.kittinunf.fuel.httpDelete
import com.github.kittinunf.fuel.httpGet
import com.github.kittinunf.fuel.httpPost
import com.github.kittinunf.fuel.jackson.jacksonDeserializerOf
import lit.fass.server.helper.TestTypes.ApiTest
import lit.fass.server.helper.TestcontainerSupport
import lit.fass.server.persistence.CollectionConfigPersistenceService.Companion.COLLECTION_CONFIG_TABLE
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.*
import org.junit.jupiter.api.MethodOrderer.OrderAnnotation

/**
 * @author Michael Mair
 */
@Tag(ApiTest)
@TestMethodOrder(OrderAnnotation::class)
internal class ConfigsApiTest : TestcontainerSupport() {

    @AfterAll
    fun cleanupTests() {
        clearTable(COLLECTION_CONFIG_TABLE)
    }

    @Test
    @Order(1)
    fun `configs POST endpoint`() {
        // create config with kotlin script
        "/configs"
            .httpPost()
            .authentication().basic("admin", "admin")
            .body(
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
            .response()
            .apply {
                val response = second
                assertThat(response.statusCode).isEqualTo(204)
            }
        // update config with groovy script
        "/configs"
            .httpPost()
            .authentication().basic("admin", "admin")
            .body(
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
            .response()
            .apply {
                val response = second
                assertThat(response.statusCode).isEqualTo(204)
            }
    }

    @Test
    @Order(2)
    fun `configs POST endpoint schedules config`() {
        // update config with schedule and retention
        "/configs"
            .httpPost()
            .authentication().basic("admin", "admin")
            .body(
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
            .response()
            .apply {
                val response = second
                assertThat(response.statusCode).isEqualTo(204)
            }

        assertThat(litfassServer.logs).contains(
            "Creating scheduled collection job foo with cron * * * * * ? *",
            "Creating scheduled collection job foo with cron * * * * * ? *",
            "Collection job foo to be scheduled every second",
            "Creating scheduled retention job foo with cron 0 0 0 ? * SUN *",
            "Retention job foo to be scheduled at 00:00 at Sunday day"
        )
    }

    @Test
    @Order(3)
    fun `configs GET endpoint`() {
        "/configs"
            .httpGet()
            .authentication().basic("admin", "admin")
            .responseObject<List<Map<String, Any?>>>(jacksonDeserializerOf())
            .apply {
                val result = third.component1()!!
                assertThat(result).hasSize(2)
                assertThat(result[0]["collection"]).isEqualTo("foo")
                assertThat(result[0]["flows"]).isNotNull
                assertThat(result[1]["collection"]).isEqualTo("bar")
                assertThat(result[1]["flows"]).isNotNull
            }
    }

    @Test
    @Order(4)
    fun `configs {collection} GET endpoint`() {
        "/configs/foo"
            .httpGet()
            .authentication().basic("admin", "admin")
            .responseObject<Map<String, Any?>>(jacksonDeserializerOf())
            .apply {
                val result = third.component1()!!
                assertThat(result["collection"]).isEqualTo("foo")
                assertThat(result["flows"] is Collection<*>).isTrue()
                assertThat(result["flows"] as Collection<*>).hasSize(1)
            }
    }

    @Test
    @Order(5)
    fun `configs {collection} DELETE endpoint`() {
        "/configs/foo"
            .httpDelete()
            .authentication().basic("admin", "admin")
            .response()
            .apply {
                val response = second
                assertThat(response.statusCode).isEqualTo(204)
            }
        "/configs/bar"
            .httpDelete()
            .authentication().basic("admin", "admin")
            .response()
            .apply {
                val response = second
                assertThat(response.statusCode).isEqualTo(204)
            }
    }

}