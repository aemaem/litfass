package lit.fass.litfass.server.persistence

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import javax.sql.DataSource


/**
 * @author Michael Mair
 */
class JdbcDataSource(url: String, username: String, password: String, properties: Map<String, Any> = emptyMap()) {
    private val dataSource = HikariDataSource(HikariConfig().apply {
        this.jdbcUrl = url
        this.username = username
        this.password = password
        properties.forEach { name, value ->
            addDataSourceProperty(name, value)
        }
    })

    fun instance(): DataSource {
        return dataSource
    }
}
