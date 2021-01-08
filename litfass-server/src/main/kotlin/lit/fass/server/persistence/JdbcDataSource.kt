package lit.fass.server.persistence

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import javax.sql.DataSource


/**
 * @author Michael Mair
 */
class JdbcDataSource(
    url: String,
    private val database: String,
    username: String,
    password: String,
    poolSize: Int,
    properties: Map<String, Any> = emptyMap()
) {
    private val dataSource = HikariDataSource(HikariConfig().apply {
        this.jdbcUrl = "$url/$database"
        this.username = username
        this.password = password
        this.isAutoCommit = true
        this.maximumPoolSize = poolSize
        properties.forEach { name, value ->
            addDataSourceProperty(name, value)
        }
    })

    fun instance(): DataSource {
        return dataSource
    }

    fun database(): String {
        return database
    }

    fun close() {
        dataSource.close()
    }
}
