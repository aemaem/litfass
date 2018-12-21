package lit.fass.litfass.server.persistence.postgres

import com.fasterxml.jackson.databind.ObjectMapper
import com.google.common.base.Charsets.UTF_8
import com.google.common.hash.Hashing.murmur3_128
import lit.fass.litfass.server.persistence.Datastore
import lit.fass.litfass.server.persistence.Datastore.POSTGRES
import lit.fass.litfass.server.persistence.JdbcDataSource
import lit.fass.litfass.server.persistence.PersistenceException
import lit.fass.litfass.server.persistence.PersistenceService
import lit.fass.litfass.server.persistence.PersistenceService.Companion.ID_KEY
import org.apache.commons.lang3.RandomStringUtils.randomAlphanumeric
import org.jooq.Configuration
import org.jooq.SQLDialect
import org.jooq.impl.DSL.using
import org.jooq.impl.DefaultConfiguration
import org.slf4j.LoggerFactory
import java.time.OffsetDateTime.now
import java.time.ZoneOffset.UTC

/**
 * @author Michael Mair
 */
class PostgresPersistenceService(private val dataSource: JdbcDataSource, private val jsonMapper: ObjectMapper) :
    PersistenceService {
    companion object {
        private val log = LoggerFactory.getLogger(this::class.java.enclosingClass)
    }

    init {
        System.setProperty("org.jooq.no-logo", true.toString())
    }

    private val jooq = using(
        DefaultConfiguration()
            .set(dataSource.instance())
            .set(SQLDialect.POSTGRES)
    )

    override fun isApplicable(datastore: Datastore): Boolean {
        return POSTGRES == datastore
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
            SET data = $collection.data || excluded.data
            """.trimIndent(),
            id ?: randomAlphanumeric(64),
            jsonMapper.writeValueAsString(data),
            now(UTC)
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