package lit.fass.server.helper

import lit.fass.server.persistence.JdbcDataSource
import org.jooq.DSLContext
import org.jooq.Record
import org.jooq.Result
import org.jooq.SQLDialect.POSTGRES
import org.jooq.impl.DSL.*
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.testcontainers.containers.CockroachContainer
import org.testcontainers.containers.wait.strategy.LogMessageWaitStrategy
import java.time.Duration

/**
 * @author Michael Mair
 */
abstract class CockroachSupport {

    val cockroach = CockroachContainer("cockroachdb/cockroach:v20.2.3").apply {
        addExposedPort(26257)
        withLogConsumer {
            print("COCKROACH: ${it.utf8String}")
        }
        withCommand("start-single-node --insecure")
    }

    lateinit var jdbcDataSource: JdbcDataSource
    lateinit var jooq: DSLContext

    @BeforeAll
    fun setupSpec() {
        cockroach.start()
        jdbcDataSource = JdbcDataSource(
            "jdbc:postgresql://${cockroach.host}:${cockroach.getMappedPort(26257)}",
            "postgres",
            "root",
            "",
            1,
            emptyMap()
        )
        jooq = using(jdbcDataSource.instance(), POSTGRES)
    }

    @AfterAll
    fun cleanupSpec() {
        jdbcDataSource.close()
        cockroach.stop()
    }

    fun dropTable(tableName: String) = jooq.dropTableIfExists(tableName).execute()

    fun clearTable(tableName: String) = jooq.delete(table(tableName)).execute()

    fun selectAllFromTable(tableName: String): Result<Record> = jooq.select().from(table(tableName)).orderBy(field("created")).fetch()
}