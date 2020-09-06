package lit.fass.server.helper

import lit.fass.server.persistence.JdbcDataSource
import org.jooq.DSLContext
import org.jooq.Record
import org.jooq.Result
import org.jooq.SQLDialect.POSTGRES
import org.jooq.impl.DSL.table
import org.jooq.impl.DSL.using
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.testcontainers.containers.PostgreSQLContainer

/**
 * @author Michael Mair
 */
abstract class PostgresSupport {

    val postgres = PostgreSQLContainer<Nothing>("${PostgreSQLContainer.IMAGE}:11.5-alpine").apply {
        addExposedPort(5432)
        withDatabaseName("litfass")
        withUsername("admin")
        withPassword("admin")
        withLogConsumer {
            print("POSTGRES: ${it.utf8String}")
        }
    }

    lateinit var jdbcDataSource: JdbcDataSource
    lateinit var jooq: DSLContext

    @BeforeAll
    fun setupClass() {
        postgres.start()
        jdbcDataSource = JdbcDataSource(
            "jdbc:postgresql://${postgres.host}:${postgres.getMappedPort(5432)}",
            "litfass",
            "admin",
            "admin",
            2,
            emptyMap()
        )
        jooq = using(jdbcDataSource.instance(), POSTGRES)
    }

    @AfterAll
    fun cleanupClass() {
        jdbcDataSource.close()
        postgres.stop()
    }

    fun dropTable(tableName: String) = jooq.dropTableIfExists(tableName).execute()

    fun clearTable(tableName: String) = jooq.delete(table(tableName)).execute()

    fun selectAllFromTable(tableName: String): Result<Record> = jooq.select().from(table(tableName)).fetch()
}