package lit.fass.server.postgres

import com.fasterxml.jackson.databind.ObjectMapper
import lit.fass.server.helper.PostgresSupport
import lit.fass.server.helper.TestTypes.IntegrationTest
import lit.fass.server.persistence.CollectionConfigPersistenceService.Companion.COLLECTION_CONFIG_TABLE
import lit.fass.server.persistence.CollectionPersistenceService.Companion.ID_KEY
import lit.fass.server.persistence.PersistenceException
import lit.fass.server.persistence.postgres.PostgresPersistenceService
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.jooq.impl.DSL.field
import org.jooq.impl.DSL.table
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.TestInstance.Lifecycle.PER_CLASS
import java.time.OffsetDateTime
import java.time.ZoneOffset.UTC

/**
 * @author Michael Mair
 */
@Tag(IntegrationTest)
@TestInstance(PER_CLASS)
internal class PostgresPersistenceServiceTest : PostgresSupport() {

    lateinit var persistenceService: PostgresPersistenceService

    @BeforeEach
    fun setup() {
        dropTable("foo")
        dropTable(COLLECTION_CONFIG_TABLE)
        persistenceService = PostgresPersistenceService(jdbcDataSource, ObjectMapper())
    }

    @Test
    fun `collection and data is saved`() {
        val collection = "foo"
        val data = listOf(
            mapOf("foo" to "bar", "bar" to true),
            mapOf("foo" to "blub", "bar" to false),
            mapOf(ID_KEY to "1", "foo" to 42, "bar" to true)
        )

        persistenceService.saveCollection(collection, data)
        val result = selectAllFromTable(collection)

        assertThat(result).hasSize(3)
        assertThat(result.getValues("id", String::class.java)[0]).matches("""[a-zA-Z0-9].+""")
        assertThat(result.getValues("data", String::class.java)[0]).isEqualTo("""{"bar": true, "foo": "bar"}""")
        assertThat(result.getValues("created", OffsetDateTime::class.java)[0]).isNotNull()
        assertThat(result.getValues("updated", OffsetDateTime::class.java)[0]).isNotNull()
        assertThat(result.getValues("id", String::class.java)[1]).matches("""[a-zA-Z0-9].+""")
        assertThat(result.getValues("data", String::class.java)[1]).isEqualTo("""{"bar": false, "foo": "blub"}""")
        assertThat(result.getValues("created", OffsetDateTime::class.java)[1]).isNotNull()
        assertThat(result.getValues("updated", OffsetDateTime::class.java)[1]).isNotNull()
        assertThat(result.getValues("id", String::class.java)[2]).isEqualTo("1")
        assertThat(result.getValues("data", String::class.java)[2]).isEqualTo("""{"id": "1", "bar": true, "foo": 42}""")
        assertThat(result.getValues("created", OffsetDateTime::class.java)[2]).isNotNull()
        assertThat(result.getValues("updated", OffsetDateTime::class.java)[2]).isNotNull()
    }

    @Test
    fun `collection and data with id is saved`() {
        val collection = "foo"
        val data = mapOf(ID_KEY to "1", "foo" to "bar", "bar" to true)

        persistenceService.saveCollection(collection, data, data["id"])
        val result = selectAllFromTable(collection)

        assertThat(result).hasSize(1)
        assertThat(result.getValues("id", String::class.java)[0]).isEqualTo("1")
        assertThat(
            result.getValues(
                "data",
                String::class.java
            )[0]
        ).isEqualTo("""{"id": "1", "bar": true, "foo": "bar"}""")
        assertThat(result.getValues("created", OffsetDateTime::class.java)[0]).isNotNull()
        assertThat(result.getValues("updated", OffsetDateTime::class.java)[0]).isNotNull()
    }

    @Test
    fun `collection and data with id is inserted and updated`() {
        val collection = "foo"
        val data = mapOf(ID_KEY to "1", "foo" to "bar", "bar" to true)

        persistenceService.saveCollection(collection, data, data["id"])
        val dataUpdate = mapOf(ID_KEY to "1", "foo" to "blub", "blub" to 100)
        persistenceService.saveCollection(collection, dataUpdate, dataUpdate["id"])
        val result = selectAllFromTable(collection)

        assertThat(result).hasSize(1)
        assertThat(result.getValues("id", String::class.java)[0]).isEqualTo("1")
        assertThat(
            result.getValues(
                "data",
                String::class.java
            )[0]
        ).isEqualTo("""{"id": "1", "bar": true, "foo": "blub", "blub": 100}""")
        assertThat(result.getValues("created", OffsetDateTime::class.java)[0]).isNotNull()
        assertThat(result.getValues("updated", OffsetDateTime::class.java)[0]).isNotNull()
    }

    @Test
    fun `collection with id is deleted`() {
        val collection = "foo"
        val data1 = mapOf(ID_KEY to "1", "foo" to "bar", "bar" to true)
        val data2 = mapOf(ID_KEY to "2", "foo" to "bar")

        @Suppress("CAST_NEVER_SUCCEEDS")
        persistenceService.saveCollection(collection, data1, data1["id"] as String)
        persistenceService.saveCollection(collection, data2, data2["id"] as String)
        persistenceService.removeCollection(collection, "1")

        val result = selectAllFromTable(collection)

        assertThat(result).hasSize(1)
        assertThat(result.getValues("id", String::class.java)[0]).isEqualTo("2")
    }

    @Test
    fun `collection and data is retrieved`() {
        persistenceService.saveCollection("foo", mapOf("foo" to "bar"), "1")
        persistenceService.saveCollection("foo", mapOf("bar" to true), "2")

        val result = persistenceService.findCollectionData("foo", "2")
        assertThat(result).extracting("bar").isEqualTo(true)
    }

    @Test
    fun `collection is deleted before a given timestamp`() {
        val now = OffsetDateTime.now(UTC)
        persistenceService.saveCollection("foo", mapOf("foo" to "bar"), "1")
        jooq.update(table("foo")).set(field("updated"), now.minusDays(1))
            .where(field("id").eq("1")).execute()
        persistenceService.saveCollection("foo", mapOf("foo" to "bar"), "2")
        jooq.update(table("foo")).set(field("updated"), now.minusDays(2))
            .where(field("id").eq("2")).execute()
        persistenceService.saveCollection("foo", mapOf("foo" to "bar"), "3")
        jooq.update(table("foo")).set(field("updated"), now.minusDays(3))
            .where(field("id").eq("3")).execute()
        persistenceService.saveCollection("foo", mapOf("foo" to "bar"), "4")
        jooq.update(table("foo")).set(field("updated"), now.minusDays(4))
            .where(field("id").eq("4")).execute()
        persistenceService.saveCollection("foo", mapOf("foo" to "bar"), "5")
        jooq.update(table("foo")).set(field("updated"), now.minusDays(5))
            .where(field("id").eq("5")).execute()

        persistenceService.deleteBefore("foo", now.minusHours(25))
        val result = selectAllFromTable("foo")

        assertThat(result).hasSize(1)
        assertThat(result.getValues("id", String::class.java)[0]).isEqualTo("1")
    }

    @Test
    fun `collection and data with wrong type of id throws exception`() {
        val collection = "foo"
        val dataBooleanId = mapOf(ID_KEY to true, "foo" to "bar", "bar" to true)
        val dataNumberId = mapOf(ID_KEY to 1, "foo" to "bar", "bar" to true)

        assertThatThrownBy { persistenceService.saveCollection(collection, dataBooleanId, dataBooleanId["id"]) }
            .isInstanceOf(PersistenceException::class.java)
        assertThatThrownBy { persistenceService.saveCollection(collection, dataNumberId, dataNumberId["id"]) }
            .isInstanceOf(PersistenceException::class.java)
    }

    @Test
    fun `collection config is saved`() {
        val collection = "foo"
        val config = """
            collection: $collection
            flows:
              - flow:
                  steps:
                    - script:
                        description: "Transform something"
                        language: kts
                        code: bindings["data"]
            """.trimIndent()

        persistenceService.saveConfig(collection, config)
        val result = selectAllFromTable(COLLECTION_CONFIG_TABLE)

        assertThat(result).hasSize(1)
        assertThat(result.getValues("config", String::class.java)[0]).isEqualTo(
            """
            collection: foo
            flows:
              - flow:
                  steps:
                    - script:
                        description: "Transform something"
                        language: kts
                        code: bindings["data"]
            """.trimIndent()
        )
    }

    @Test
    fun `collection config is found`() {
        val collection = "foo"
        val config = """
                    collection: $collection
                    flows:
                      - flow:
                          steps:
                            - script:
                                description: "Transform something"
                                language: kts
                                code: bindings["data"]
                    """.trimIndent()
        persistenceService.saveConfig(collection, config)
        val result = persistenceService.findConfig("foo")

        assertThat(result).isEqualTo(
            """
                collection: foo
                flows:
                  - flow:
                      steps:
                        - script:
                            description: "Transform something"
                            language: kts
                            code: bindings["data"]
                """.trimIndent()
        )
    }

    @Test
    fun `all collection configs are found`() {
        persistenceService.saveConfig(
            "foo1", """
        collection: foo1
        flows:
          - flow:
              steps:
                - script:
                    description: "Transform something"
                    language: kts
                    code: bindings["data"]            
        """.trimIndent()
        )
        persistenceService.saveConfig(
            "foo2", """
        collection: foo2
        flows:
          - flow:
              steps:
                - script:
                    description: "Transform something"
                    language: kts
                    code: bindings["data"]
        """.trimIndent()
        )
        persistenceService.saveConfig(
            "foo3", """
        flows:
          - flow:
              steps:
                - script:
                    description: "Transform something"
                    language: kts
                    code: bindings["data"]
        """.trimIndent()
        )

        val result = persistenceService.findConfigs()
        assertThat(result).hasSize(3)
    }

    @Test
    fun `collection config is deleted`() {
        val collection = "foo"
        val config = """
        collection: $collection
        flows:
          - flow:
              steps:
                - script:
                    description: "Transform something"
                    language: kts
                    code: bindings["data"]            
        """.trimIndent()
        persistenceService.saveConfig(collection, config)

        persistenceService.deleteConfig("foo")
        val result = selectAllFromTable(COLLECTION_CONFIG_TABLE)

        assertThat(result).isEmpty()
    }
}