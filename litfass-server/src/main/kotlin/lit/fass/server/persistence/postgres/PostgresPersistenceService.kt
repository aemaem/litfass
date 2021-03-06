package lit.fass.server.persistence.postgres

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import com.google.common.base.Charsets.UTF_8
import com.google.common.hash.Hashing.murmur3_128
import lit.fass.server.logger
import lit.fass.server.persistence.*
import lit.fass.server.persistence.CollectionConfigPersistenceService.Companion.COLLECTION_CONFIG_TABLE
import lit.fass.server.persistence.CollectionPersistenceService.Companion.ID_KEY
import org.apache.commons.lang3.RandomStringUtils.randomAlphanumeric
import org.jooq.Configuration
import org.jooq.DSLContext
import org.jooq.SQLDialect
import org.jooq.impl.DSL.*
import org.jooq.impl.DefaultConfiguration
import java.time.OffsetDateTime
import java.time.OffsetDateTime.now
import java.time.ZoneOffset.UTC

/**
 * @author Michael Mair
 */
class PostgresPersistenceService(private val dataSource: JdbcDataSource, private val jsonMapper: ObjectMapper) :
    CollectionConfigPersistenceService, CollectionPersistenceService {
    companion object {
        private val log = this.logger()
    }

    private val jooq: DSLContext

    init {
        System.setProperty("org.jooq.no-logo", true.toString())
        jooq = using(
            DefaultConfiguration()
                .set(dataSource.instance())
                .set(SQLDialect.POSTGRES)
        )
        log.debug("Creating table $COLLECTION_CONFIG_TABLE if not exists")
        jooq.execute(
            """
            CREATE TABLE IF NOT EXISTS $COLLECTION_CONFIG_TABLE (
                collection VARCHAR(50) NOT NULL PRIMARY KEY,
                config TEXT NOT NULL,
                created TIMESTAMP NOT NULL,
                updated TIMESTAMP NOT NULL
            )
            """.trimIndent()
        )
    }

    override fun isApplicable(datastore: Datastore): Boolean {
        return Datastore.POSTGRES == datastore
    }

    override fun saveCollection(collection: String, data: Map<String, Any?>, id: Any?) {
        if (id !is String?) {
            throw PersistenceException("Id must be of type string")
        }

        jooq.transaction { config ->
            createTableIfNotExists(collection, config)
            insertOrUpdateCollection(collection, data, id, config)
        }
        log.debug("Saved collection $collection")
    }

    override fun saveCollection(collection: String, data: Collection<Map<String, Any?>>) {
        data.forEach { entry ->
            saveCollection(collection, entry, entry["id"])
        }
    }

    override fun removeCollection(collection: String, ids: Collection<String>) {
        jooq.transaction { config ->
            createTableIfNotExists(collection, config)
            deleteCollection(collection, ids, config)
        }
        log.debug("Removed collection $collection")
    }

    override fun removeCollection(collection: String, id: String) {
        jooq.transaction { config ->
            createTableIfNotExists(collection, config)
            deleteCollection(collection, listOf(id), config)
        }
        log.debug("Removed collection $collection")
    }

    override fun findCollectionData(collection: String, id: String): Map<String, Any?> {
        val found = jooq.select()
            .from(table(collection))
            .where(field(ID_KEY).eq(id))
            .fetch().firstOrNull() ?: return emptyMap()
        return jsonMapper.readValue(found.getValue("data", ByteArray::class.java),
            object : TypeReference<Map<String, Any?>>() {})
    }

    override fun deleteBefore(collection: String, timestamp: OffsetDateTime) {
        jooq.delete(table(collection))
            .where(field("updated").lt(timestamp))
            .execute()
    }

    override fun saveConfig(collection: String, config: String) {
        jooq.execute(
            """
            INSERT INTO $COLLECTION_CONFIG_TABLE (collection, config, created, updated)
            VALUES ({0}, {1}, {2}, {2})
            ON CONFLICT (collection) DO UPDATE
            SET config = excluded.config,
                updated = excluded.updated
            """.trimIndent(),
            collection,
            config,
            now(UTC).toLocalDateTime()
        )
    }

    override fun findConfig(collection: String): String? {
        val found = jooq.select(field("config"))
            .from(table(COLLECTION_CONFIG_TABLE))
            .where(field("collection").eq(collection))
            .fetch().firstOrNull() ?: return null
        return found[0].toString()
    }

    override fun findConfigs(): List<String?> {
        return jooq.select(field("config"))
            .from(table(COLLECTION_CONFIG_TABLE))
            .orderBy(field("collection"))
            .fetch()
            .map { it[0].toString() }
    }

    override fun deleteConfig(collection: String) {
        jooq.delete(table(COLLECTION_CONFIG_TABLE))
            .where(field("collection").eq(collection))
            .execute()
    }

    private fun insertOrUpdateCollection(
        collection: String,
        data: Map<String, Any?>,
        id: String?,
        config: Configuration
    ) {
        using(config).execute(
            """
            INSERT INTO $collection ($ID_KEY, data, created, updated)
            VALUES ({0}, {1}::jsonb, {2}, {2})
            ON CONFLICT ($ID_KEY) DO UPDATE
            SET data = $collection.data || excluded.data,
                updated = excluded.updated
            """.trimIndent(),
            id ?: randomAlphanumeric(64),
            jsonMapper.writeValueAsString(data),
            now(UTC).toLocalDateTime()
        )
    }

    private fun deleteCollection(
        collection: String,
        ids: Collection<String>,
        config: Configuration
    ) {
        using(config).execute(
            """
            DELETE FROM $collection WHERE $ID_KEY IN ({0})
            """.trimIndent(),
            ids.joinToString()
        )
    }

    private fun createTableIfNotExists(tableName: String, config: Configuration) {
        if (existsTable(tableName, config)) {
            return
        }

        val tableNameHash = murmur3_128(44).hashString(tableName, UTF_8)
        using(config).execute(
            """
            CREATE TABLE $tableName (
                $ID_KEY VARCHAR(128) NOT NULL PRIMARY KEY,
                data JSONB,
                created TIMESTAMP NOT NULL,
                updated TIMESTAMP NOT NULL
            );
            CREATE INDEX idx_created_${tableNameHash} ON $tableName (created);
            CREATE INDEX idx_updated_${tableNameHash} ON $tableName (updated);
            """.trimIndent()
        )
    }

    private fun existsTable(tableName: String, config: Configuration): Boolean {
        return using(config).resultQuery(
            """
            SELECT EXISTS (
                SELECT 1
                FROM information_schema.tables
                WHERE table_catalog = '${dataSource.database()}'
                AND table_name = '$tableName'
            );
            """.trimIndent()
        ).fetchSingleInto(Boolean::class.java)
    }
}