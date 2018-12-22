package lit.fass.litfass.server.helper

import lit.fass.litfass.server.persistence.JdbcDataSource
import org.jooq.DSLContext
import org.jooq.Record
import org.jooq.Result
import org.jooq.SQLDialect

import static org.jooq.impl.DSL.table
import static org.jooq.impl.DSL.using

/**
 * @author Michael Mair
 */
trait PostgresSupport {

    JdbcDataSource jdbcDataSource = new JdbcDataSource("jdbc:postgresql://localhost:5432", "litfass", "admin", "admin", [:])
    DSLContext jooq = using(jdbcDataSource.instance(), SQLDialect.POSTGRES)

    void dropTable(String tableName) {
        jooq.dropTableIfExists(tableName)
                .execute()
    }

    Result<Record> selectAllFromTable(String tableName) {
        return jooq.select()
                .from(table(tableName))
                .fetch()
    }
}
