package lit.fass.server.helper

import lit.fass.server.persistence.JdbcDataSource
import org.jooq.Record
import org.jooq.Result
import org.jooq.SQLDialect.POSTGRES
import org.jooq.impl.DSL.table
import org.jooq.impl.DSL.using
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers

/**
 * @author Michael Mair
 */
abstract class PostgresSupport {

    val jdbcDataSource = JdbcDataSource("jdbc:postgresql://localhost:5432", "litfass", "admin", "admin", 2, emptyMap())
    val jooq = using(jdbcDataSource.instance(), POSTGRES)

    fun dropTable(tableName: String) = jooq.dropTableIfExists(tableName).execute()

    fun clearTable(tableName: String) = jooq.delete(table(tableName)).execute()

    fun selectAllFromTable(tableName: String): Result<Record> = jooq.select().from(table(tableName)).fetch()
}