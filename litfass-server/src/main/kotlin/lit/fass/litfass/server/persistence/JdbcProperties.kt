package lit.fass.litfass.server.persistence

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.stereotype.Component

/**
 * @author Michael Mair
 */
@Component
@ConfigurationProperties(prefix = "litfass.jdbc")
class JdbcProperties {

    var url: String = ""
    var database: String = ""
    var username: String = ""
    var password: String = ""
    var poolSize: Int = 1
}