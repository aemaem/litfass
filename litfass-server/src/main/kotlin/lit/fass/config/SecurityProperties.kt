package lit.fass.config

import org.springframework.boot.context.properties.ConfigurationProperties

/**
 * @author Michael Mair
 */
@ConfigurationProperties(prefix = "litfass.security")
class SecurityProperties {

    var users: Map<String, String> = emptyMap()
}