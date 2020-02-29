package lit.fass.litfass.server.config

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.stereotype.Component

/**
 * @author Michael Mair
 */
@Component
@ConfigurationProperties(prefix = "litfass.config")
class ConfigProperties {

    var collectionPath: String = ""
}